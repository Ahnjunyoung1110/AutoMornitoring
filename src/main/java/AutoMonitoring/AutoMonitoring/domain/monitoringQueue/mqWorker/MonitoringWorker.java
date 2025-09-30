package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
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
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

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


    // DB 저장 후 모니터링 시작 메시지를 수신
    @RabbitListener(queues = RabbitNames.Q_STAGE3)
    void startMessage(ProgramInfo info){
        log.info("모니터링 시작 메시지 수신. TraceId: {}", info.getTraceId());
        for(String key: info.getResolutionToUrl().keySet()){
            String redisKey = RedisKeys.state(info.getTraceId(), key);
            redisService.setValues(redisKey, "MONITORING");

            StartMonitoringDTO dto = new StartMonitoringDTO(info.getTraceId(), info.getResolutionToUrl().get(key) , key, info.getUserAgent());
            monitoringService.startMornitoring(dto);
        }
        log.info("모니터링을 시작합니다. TraceId: {}, 대상 해상도: {}", info.getTraceId(), info.getResolutionToUrl().keySet());
        redisService.setValues(info.getTraceId(), "MONITORING");
    }


    // 주기적인 모니터링 작업을 수행. 실패 시 재시도 큐로 보냄.
    @RabbitListener(id = "Monitoring_worker",queues = RabbitNames.Q_WORK, concurrency = "5", containerFactory="probeContainerFactory"
    )
    void receiveMessage(CheckMediaManifestCmd cmd){
        log.info("모니터링 작업 수신. TraceId: {}, Resolution: {}", cmd.traceId(), cmd.resolution());

        String lockKey = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());
        Duration ttl = Duration.ofMinutes(3L); // 워커가 비정상 종료될 경우를 대비한 TTL
        boolean lockAcquired = redisService.getOpsAbsent(lockKey, "1", ttl);

        if (!lockAcquired) {
            log.info("다른 워커가 이미 작업을 수행 중입니다. Key: {}", lockKey);
            return; // Lock 획득에 실패하면 중복 작업이므로 즉시 종료
        }

        try{
            Instant nowTime = Instant.now();
            log.info("목표 시간과의 차이: {}ms", Duration.between(cmd.publishTime(), nowTime).toMillis());

            // 1. 미디어 매니페스트 다운로드
            String media = getMediaService.getMedia(cmd.mediaUrl(), cmd.userAgent());
            Instant getTime = Instant.now();
            Duration requestDuration = Duration.between(nowTime, getTime);

            // 2. 매니페스트 파싱
            RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(media, requestDuration, cmd.traceId(), cmd.resolution());
            log.info("작업 처리 완료. 처리 시간: {}ms", Duration.between(nowTime, Instant.now()).toMillis());

            // 3. Redis에 결과 저장
            addToRedis(cmd.traceId(), cmd.resolution(), recordMediaToRedisDTO);

            // 4. 다음 작업 스케줄링
            sendDelay(cmd);

        } catch (Exception e) {
            log.error("모니터링 작업 실패. 재시도 큐로 보냅니다. TraceId: {}, Resolution: {}, Error: {}", cmd.traceId(), cmd.resolution(), e.getMessage());

            // 재시도 상태(1/5)를 Redis에 기록
            String redisKey = RedisKeys.state(cmd.traceId(), cmd.resolution());
            redisService.setValues(redisKey, "RETRYING (1/5)");

            // 실패 시 AmqpRejectAndDontRequeueException을 발생시켜 메시지를 DLX로 보냄
            throw new AmqpRejectAndDontRequeueException("Monitoring task failed", e);
        } finally {
            redisService.deleteValues(lockKey);
            log.debug("작업 완료 후 Lock을 해제합니다. Key: {}", lockKey);
        }
    }

    // redis에 매니페스트 이력과 현재 상태를 저장
    void addToRedis(String traceId, String resolution ,RecordMediaToRedisDTO recordMediaToRedisDTO){
        redisMediaService.pushHistory(traceId, resolution, recordMediaToRedisDTO, 10);
        redisMediaService.saveState(traceId, resolution, recordMediaToRedisDTO);
    }

    // 다음 모니터링 작업을 위해 지연 메시지를 전송
    void sendDelay(CheckMediaManifestCmd cmd){
        Instant now = Instant.now();
        final long baseDelay = 5_000L; // 기본 딜레이 5초

        Instant prevDue = cmd.publishTime() != null ? cmd.publishTime() : now;
        Instant nextDue = prevDue.plusMillis(baseDelay);
        long skew = Math.floorMod(cmd.traceId().hashCode(), 700); // 0~699ms 고정 지터
        nextDue = nextDue.plusMillis(skew);

        final long delay = Math.max(Duration.between(now, nextDue).toMillis(), 100L);

        CheckMediaManifestCmd newCmd;
        // 지연 시간이 너무 길거나, 이미 시간이 지났으면 즉시 실행
        if((now.isAfter(nextDue)) || (delay <= 100L)){
            newCmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(), cmd.userAgent(), 0, now, cmd.traceId());
            log.warn("스케줄이 지연되어 즉시 실행합니다. TraceId: {}, Resolution: {}", cmd.traceId(), cmd.resolution());
            rabbit.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK, newCmd);
            return;
        }

        log.info("{}ms 후 다음 작업을 스케줄링합니다.", delay);
        newCmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(), cmd.userAgent(), 0, nextDue, cmd.traceId());
        String delayRoutingKey = getDelayRoutingKey(delay);

        // EX_DELAY Exchange로 메시지를 보내, TTL이 설정된 큐로 들어가게 함
        rabbit.convertAndSend(RabbitNames.EX_DELAY, delayRoutingKey, newCmd, m -> {
            m.getMessageProperties().setExpiration(String.valueOf(delay));
            return m;
        });
    }

    // 지연 시간에 따라 적절한 라우팅 키를 반환
    private String getDelayRoutingKey(long delayMs) {
        return switch((int)(delayMs / 1000)) {
            case 0, 1 -> RabbitNames.RK_DELAY_1S;
            case 2 -> RabbitNames.RK_DELAY_2S;
            case 3 -> RabbitNames.RK_DELAY_3S;
            case 4 -> RabbitNames.RK_DELAY_4S;
            default -> RabbitNames.RK_DELAY_DEFAULT;
        };
    }
}