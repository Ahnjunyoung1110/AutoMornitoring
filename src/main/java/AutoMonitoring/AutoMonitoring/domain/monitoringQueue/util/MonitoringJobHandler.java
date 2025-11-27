package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramRefreshRequestCommand;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception.SessionExpiredException;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringJobHandler {

    private final GetMediaService getMediaService;     // WebClient
    private final ParseMediaManifest parseMediaManifest;
    private final RedisService redisService;           // 동기/비동기 메서드 모두 포함
    private final RabbitTemplate rabbit;               // 동기 (sendDelay에서 사용)
    private final Sender sender;                       // 비동기 발송용
    private final ObjectMapper objectMapper;           // DTO 직렬화용

    public Mono<Void> handle(CheckMediaManifestCmd cmd) {
        String lockKey = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());
        Instant start = Instant.now();
        String epochKey = RedisKeys.messageEpoch(cmd.traceId());

        return redisService.getEpochReactive(epochKey)
            .flatMap(nowEpoch -> {
                // 이전 버전의 메시지인 경우 폐기
                if (!nowEpoch.equals(cmd.epoch())) {
                    log.info("Stale message. drop. traceId={}, epoch={}, now={}",
                            cmd.traceId(), cmd.epoch(), nowEpoch);
                    return Mono.empty();
                }

                // 작업을 시작하기 전 Redis에서 Lock 을 비동기적으로 획득하는 Mono
                Mono<Boolean> acquired = redisService.getOpsAbsentReactive(lockKey, "1", Duration.ofMinutes(3));

                // 작업을 끝낸 후 Redis에서 Lock을 비동기적으로 해제하는 Mono
                Mono<Void> release = redisService.deleteValuesReactive(lockKey)
                        .onErrorResume(e -> {
                            log.warn("release fail: {}", e.toString());
                            return Mono.empty();
                        })
                        .then();

                // Submanifest를 비동기적으로 받아오는 Mono
                Mono<String> getMedia = getMediaService.getMediaNonBlocking(cmd.mediaUrl(), cmd.userAgent(), cmd.traceId())
                        .onErrorResume(e -> {
                            if (e instanceof SessionExpiredException se) {
                                log.warn("세션 만료 발생: trateId={}", se.getTraceId());
                                return redisService.nextEpochReactive(epochKey)
                                    .flatMap(nextEpoch -> {
                                        ProgramRefreshRequestCommand command = new ProgramRefreshRequestCommand(cmd.traceId());
                                        return Mono.fromRunnable(() -> rabbit.convertAndSend(RabbitNames.EX_PROGRAM_COMMAND, RabbitNames.RK_PROGRAM_COMMAND, command))
                                                   .subscribeOn(Schedulers.boundedElastic())
                                                   .then(Mono.error(e));
                                    });
                            }
                            log.warn("가져오기에 실패했습니다. {}", e.toString());
                            return Mono.error(e);
                        });

                // m3u8의 valid를 확인하기 위해서 rabbitMQ로 checkMediaValid 도메인에 message를 보내고 이후의 메시지를 스케줄링 하는 함수
                Function<CheckValidDTO, Mono<Void>> checkValid =
                        dto -> {
                            // The send operation
                            Mono<Void> sendToValidationQueue = Mono.fromCallable(() -> {
                                        int partition = calcPartition(dto.traceId(), dto.resolution());
                                        String routingKey = RabbitNames.routingValid(partition);
                                        byte[] body = objectMapper.writeValueAsBytes(dto);
                                        return new OutboundMessage(RabbitNames.EX_VALID, routingKey, body);
                                    })
                                    .flatMap(outboundMessage -> sender.send(Mono.just(outboundMessage)));

                            // The sendDelay call is still blocking
                            // I'll execute it after the main send is successful
                            return sendToValidationQueue
                                    .doOnSuccess(v -> {
                                        sendDelay(cmd);
                                        log.info("처리 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
                                    });
                        };

                return acquired
                        .filter(Boolean::booleanValue)
                        .flatMap(ig -> getMedia)
                        .map(media -> {
                            Duration took = Duration.between(start, Instant.now());
                            return parseMediaManifest.parse(media, took, cmd.traceId(), cmd.resolution());
                        })
                        .flatMap(checkValid)
                        .onErrorResume(e -> release.then(Mono.error(e)))
                        .then(release);
            });
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
        if ((now.isAfter(nextDue)) || (delay == 100L)) {
            newCmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(), cmd.userAgent(), 0, now, cmd.traceId(), cmd.epoch());
            log.warn("스케줄이 지연되어 즉시 실행합니다. TraceId: {}, Resolution: {}", cmd.traceId(), cmd.resolution());
            rabbit.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK, newCmd);
            return;
        }

        log.info("{}ms 후 다음 작업을 스케줄링합니다.", delay);
        newCmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(), cmd.userAgent(), 0, nextDue, cmd.traceId(), cmd.epoch());
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


    // valid 를 체크할 큐를 정하는 라우팅
    private int calcPartition(String traceId, String resolution) {
        int hash = Objects.hash(traceId, resolution);
        return Math.floorMod(hash, RabbitNames.VALID_PARTITIONS);
    }
}