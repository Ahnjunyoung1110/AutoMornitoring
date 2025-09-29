package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.dto.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Component
@Slf4j
public class MonitoringWorker {

    private final ParseMediaManifest parseMediaManifest;
    private final GetMediaService getMediaService;
    private final RedisService redisService;
    private final RabbitTemplate rabbit;
    private final RedisMediaService redisMediaService;
    private final MonitoringService monitoringService;


    // DB를 저장한 후 모니터링을 시작하는 함수
    @RabbitListener(queues = RabbitNames.Q_STAGE3)
    void startMessage(ProgramInfo info){

        for(String key: info.getResolutionToUrl().keySet()){
            StartMonitoringDTO dto = new StartMonitoringDTO(info.getTraceId(), info.getResolutionToUrl().get(key) , key, info.getUserAgent());
            monitoringService.startMornitoring(dto);
        }

        log.info("모니터링을 시작합니다. %s,%s".formatted(info.getTraceId(),info.getResolutionToUrl()));
    }



    // 내부에서 계속 돌아가는 메시지를 받아 처리하는 함수, 처리에 실패하면 딜레이 큐로 보낸다.
    @RabbitListener(queues = RabbitNames.WORK_QUEUE, concurrency = "5", containerFactory="probeContainerFactory")
    void receiveMessage(CheckMediaManifestCmd cmd){
        log.info("메시지를 수신했습니다. %s, %s".formatted(cmd.traceId(), cmd.resolution()));


        String lockKey = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());
        Duration ttl = Duration.ofMinutes(3L); // 워커가 비정상 종료될 경우를 대비한 TTL
        boolean lockAcquired = redisService.getOpsAbsent(lockKey, "1", ttl);

        if (!lockAcquired) {
            log.info("다른 워커가 이미 작업을 수행 중입니다. Key: %s".formatted(lockKey));
            return; // Lock 획득에 실패하면 중복 작업이므로 즉시 종료
        }

        try{
            log.info("일딴 curl 날리기 전이요~");
            Instant nowTime = Instant.now();
            Duration haha = Duration.between(cmd.publishTime(), nowTime);
            log.info("이만큼이나 목표 시간과의 차이가 있어요~ "+ haha.toMillis());

            // curl
            String media = getMediaService.getMedia(cmd.mediaUrl(), cmd.userAgent());

            Instant getTime = Instant.now();
            Duration requestDurationMs = Duration.between(nowTime,getTime);
            log.info("요청하는데 오래 걸린건가? 왜 한번씩... "+ requestDurationMs.toMillis());

            // 파싱
            RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(media, requestDurationMs,cmd.traceId(), cmd.resolution());
            

            Long duration = Duration.between(nowTime, Instant.now()).toMillis();
            log.info("메시지를 처리하는데 이만큼 걸렸습니다. " + String.valueOf(duration));

            // redis에 저장
            addToRedis(cmd.traceId(),cmd.resolution(),recordMediaToRedisDTO);
            sendDelay(cmd);


        }
        finally {
            redisService.deleteValues(lockKey);
            log.info("작업 완료 후 Lock을 해제합니다. Key: %s".formatted(lockKey));
        }

    }

    // redis에 메니페스트를 저장
    void addToRedis(String traceId, String resolution ,RecordMediaToRedisDTO recordMediaToRedisDTO){
        redisMediaService.pushHistory(traceId, resolution, recordMediaToRedisDTO, 10);
        redisMediaService.saveState(traceId, resolution, recordMediaToRedisDTO);
    }

    // 딜레이를 설정해서 queue 에 입력
    void sendDelay(CheckMediaManifestCmd cmd){
        Instant now = Instant.now();
        // 기본 딜레이는 5초
        final long base = 5_000L;

        Instant prevDue = cmd.publishTime() != null ? cmd.publishTime() : Instant.now();
        Instant nextDue = prevDue.plusMillis(base);
        long skew = Math.floorMod(cmd.traceId().hashCode(), 700); // 0~699ms 고정 지터
        nextDue = nextDue.plusMillis(skew);
        final long delay = Math.max(Duration.between(Instant.now(), nextDue).toMillis(),100L);

        CheckMediaManifestCmd newcmd;
        if((now.toEpochMilli() > nextDue.toEpochMilli()) || (delay == 100L)){
            newcmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(), cmd.userAgent(), 0, now, cmd.traceId());
            log.warn("시간이 많이 다르네잉...");
            rabbit.convertAndSend(RabbitNames.EX_PIPELINE, RabbitNames.WORK_STAGE1, newcmd);
            return;
        }
        log.info("duration: " + String.valueOf(delay));

        newcmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(), cmd.userAgent(), 0, nextDue, cmd.traceId());
        String delayQueue = getDelayQueue(delay);
        rabbit.convertAndSend("", delayQueue, newcmd, m -> {
            m.getMessageProperties().setExpiration(String.valueOf(delay));
            long nowLong = System.currentTimeMillis();
            m.getMessageProperties().setHeader("x-sent-at", nowLong);
            m.getMessageProperties().setHeader("x-due-at", nowLong + delay);
            return m;
        });
    }

    private String getDelayQueue(long delayMs) {
        return switch((int)(delayMs / 1000)) {
            case 0, 1 -> RabbitNames.ONLY_DELAY_QUEUE_1S;
            case 2 -> RabbitNames.ONLY_DELAY_QUEUE_2S;
            case 3 -> RabbitNames.ONLY_DELAY_QUEUE_3S;
            case 4 -> RabbitNames.ONLY_DELAY_QUEUE_4S;
            default -> RabbitNames.ONLY_DELAY_QUEUE;
        };
    }

//    // validate 테스트를 위해 메시지를 전달
//    void sendMessageToValid(String traceId, String resolution){
//        CheckValidDTO send = new CheckValidDTO(traceId, resolution);
//        rabbit.convertAndSend(RabbitNames.EX_PIPELINE, RabbitNames.RK_VALID, send);
//    }

}