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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

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
    @RabbitListener(queues = RabbitNames.DELAY_STAGE1, concurrency = "6-12")
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
            Instant nowTime = Instant.now();
            // curl
            String media = getMediaService.getMedia(cmd.mediaUrl(), cmd.userAgent());

            Instant getTime = Instant.now();
            Duration requestDurationMs = Duration.between(nowTime,getTime);

            RecordMediaToRedisDTO recordMediaToRedisDTO;

            // 파싱

            recordMediaToRedisDTO = parseMediaManifest.parse(media, requestDurationMs,cmd.traceId(), cmd.resolution());
            

            Long duration = Duration.between(nowTime, Instant.now()).toMillis();
            log.info("메시지를 처리하는데 이만큼 걸렸습니다. " + String.valueOf(duration));
            // redis에 저장
            addToRedis(cmd.traceId(),cmd.resolution(),recordMediaToRedisDTO);

            CheckMediaManifestCmd newCmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(),cmd.userAgent() ,0, nowTime, cmd.traceId());
            long base = 5_000L;
            long elapsed = Duration.between(cmd.publishTime(), Instant.now()).toMillis();

            log.info("딜레이 시간 " + String.valueOf(elapsed));
//            String workerFlag = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());

//            redisService.deleteValues(workerFlag);
//            // redis로 flag를 확인
//            boolean first = checkQueueAndPublish(cmd.traceId(), cmd.resolution());
//            if(!first){
//                log.info("해당 메시지가 이미 큐에 존재합니다.");
//                return;
//            }

//        // validate test 메시지 발행
//        sendMessageToValid(cmd.traceId(),cmd.resolution());

            // 딜레이(5초 - 이전시간 걸린 딜레이) 메시지 생성 후 저장
            rabbit.convertAndSend(RabbitNames.DELAY_PIPELINE, RabbitNames.DRK_STAGE1, newCmd,
                    m -> {m.getMessageProperties().setDelayLong(base); return m; }
            );
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

//    // validate 테스트를 위해 메시지를 전달
//    void sendMessageToValid(String traceId, String resolution){
//        CheckValidDTO send = new CheckValidDTO(traceId, resolution);
//        rabbit.convertAndSend(RabbitNames.EX_PIPELINE, RabbitNames.RK_VALID, send);
//    }

    // queue에서 메시지를 꺼낼때의 redis flag를 설정
//    boolean convertFlag(String traceId, String resolution){
//        String workerFlag = RedisKeys.workerLock(traceId, resolution);
//        Duration ttl = Duration.ofMinutes(3L);
//        boolean firstWorker = redisService.getOpsAbsent(workerFlag, "1", ttl);
//        if(firstWorker){
//            log.info("작업을 수행합니다.");
//            log.info("Key : %s".formatted(workerFlag));
//            return true;
//        }
//        else{
//            log.info("중복된 작업이 존재합니다. ");
//            log.info("Key : %s".formatted(workerFlag));
//            return false;
//        }
//    }
//
//    // queue에 메시지가 있는지 확인하고 없다면 메시지를 발행
//    boolean checkQueueAndPublish(String traceId, String resolution){
//        String queueFlag = RedisKeys.queueFlag(traceId, resolution);
//        String workerFlag = RedisKeys.workerLock(traceId, resolution);
//        String luaScript = """
//        if redis.call('exists', KEYS[1]) == 1 then
//            return 0
//        else
//            redis.call('setex', KEYS[1], ARGV[1], '1')
//            return 1
//        end
//        """;
//
//
//        boolean firstQueue = redisService.execute(luaScript, queueFlag, "300");
//        if(firstQueue){
//            log.info("큐에 작업을 삽입합니다.");
//            log.info("Key : %s".formatted(queueFlag));
//            return true;
//        }
//        else{
//            log.info("중복된 작업이 큐 내에 존재합니다. ");
//            log.info("Key : %s".formatted(queueFlag));
//            redisService.deleteValues(workerFlag);
//            return false;
//        }
//
//
//    }



}