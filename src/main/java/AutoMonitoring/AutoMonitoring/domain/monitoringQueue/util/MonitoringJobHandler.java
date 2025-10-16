package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringJobHandler {

    private final GetMediaService getMediaService;     // WebClient
    private final ParseMediaManifest parseMediaManifest;
    private final RedisService redisService;           // 동기
    private final RedisMediaService redisMediaService; // 동기
    private final RabbitTemplate rabbit;               // 동기 (sendDelay에서 사용)

    public Mono<Void> handle(CheckMediaManifestCmd cmd) {
        String lockKey = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());
        Instant start = Instant.now();
        log.info("시작은 했습니다.");

        // 작업을 시작하기 전 Redis에서 Lock 을 비동기적으로 획득하는 Mono
        Mono<Boolean> acquired = Mono.fromCallable(() -> redisService.getOpsAbsent(lockKey, "1", Duration.ofMinutes(3)))
                .subscribeOn(Schedulers.boundedElastic());

        // 작업을 끝낸 후 Redisd에서 Lock을 비동기적으로 해제하는 Mono
        Mono<Void> release = Mono.fromRunnable(() -> redisService.deleteValues(lockKey))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.warn("release fail: {}", e.toString());
                    return Mono.empty();}
                )
                .then();


        // Submanifest를 비동기적으로 받아오는 Mono
        Mono<String> getMedia = getMediaService.getMediaNonBlocking(cmd.mediaUrl(), cmd.userAgent())
                .onErrorResume(e -> {
                    log.warn("가져오기에 실패했습니다. {}", e.toString());
                    return Mono.empty();
                });

        // subManifest 를 redis에 비동기적으로 저장하는 함수
        Function<RecordMediaToRedisDTO, Mono<Void>> saveState =
                dto -> Mono.fromRunnable(() -> {
                            redisMediaService.pushHistory(cmd.traceId(), cmd.resolution(), dto, 10);
                            redisMediaService.saveState(cmd.traceId(), cmd.resolution(), dto);
                            sendDelay(cmd);
                            log.info("처리 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .then();

        return acquired
                .filter(Boolean::booleanValue)
                .flatMap(ig -> getMedia)
                .map(media -> {
                    Duration took = Duration.between(start, Instant.now());
                    return parseMediaManifest.parse(media, took, cmd.traceId(), cmd.resolution());
                })
                .flatMap(saveState)
                // 에러시 기존 lock 해제
                .onErrorResume(e -> release.then(Mono.error(e)))
                // 성공시 기존 lock 해제
                .then(release);
    }


    // redis에 매니페스트 이력과 현재 상태를 저장
    void addToRedis(String traceId, String resolution, RecordMediaToRedisDTO recordMediaToRedisDTO) {
        redisMediaService.pushHistory(traceId, resolution, recordMediaToRedisDTO, 10);
        redisMediaService.saveState(traceId, resolution, recordMediaToRedisDTO);
    }

    // 다음 모니터링 작업을 위해 지연 메시지를 전송
    void sendDelay(CheckMediaManifestCmd cmd) {
        Instant now = Instant.now();
        final long baseDelay = 5_000L; // 기본 딜레이 5초

        Instant prevDue = cmd.publishTime() != null ? cmd.publishTime() : now;
        Instant nextDue = prevDue.plusMillis(baseDelay);
        long skew = Math.floorMod(cmd.traceId().hashCode(), 700); // 0~699ms 고정 지터
        nextDue = nextDue.plusMillis(skew);

        final long delay = Math.max(Duration.between(now, nextDue).toMillis(), 100L);

        CheckMediaManifestCmd newCmd;
        // 지연 시간이 너무 길거나, 이미 시간이 지났으면 즉시 실행
        if ((now.isAfter(nextDue)) || (delay <= 100L)) {
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
        return switch ((int) (delayMs / 1000)) {
            case 0, 1 -> RabbitNames.RK_DELAY_1S;
            case 2 -> RabbitNames.RK_DELAY_2S;
            case 3 -> RabbitNames.RK_DELAY_3S;
            case 4, 5 -> RabbitNames.RK_DELAY_4S;
            default -> RabbitNames.RK_DELAY_DEFAULT;
        };
    }
}