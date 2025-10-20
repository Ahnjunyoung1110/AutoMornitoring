package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util.MonitoringJobHandler;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class MonitoringWorker {
    private final RedisService redisService;
    private final MonitoringService monitoringService;

    private final RabbitTemplate rabbitTemplate;
    private final Receiver receiver;
    private final Sender sender;

    private final MonitoringJobHandler handler;
    private final ObjectMapper objectMapper;


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


    @PostConstruct
    public void startMonitoring(){
        int prefetch = 52;
        int concurrency = 512;
        log.info("PostConstruct 가 실행되었습니다.");

        receiver.consumeManualAck(RabbitNames.Q_WORK, new ConsumeOptions().qos(prefetch))
                .flatMap(delivery -> {
                    byte[] body = delivery.getBody();
                    CheckMediaManifestCmd cmd = null;
                    try {
                        cmd = objectMapper.readValue(delivery.getBody(), CheckMediaManifestCmd.class);
                        System.out.printf(String.valueOf(cmd));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    CheckMediaManifestCmd finalCmd = cmd;
                    return handler.handle(cmd)
                            .doOnSuccess(__ -> delivery.ack())
                            .onErrorResume(e -> {
                                log.error("모니터링 작업 실패. 재시도 큐로 보냅니다. TraceId: {}, Resolution: {}, Error: {}", finalCmd.traceId(), finalCmd.resolution(), e.getMessage());

                                // 재시도 상태(1/5)를 Redis에 기록
                                String redisKey = RedisKeys.state(finalCmd.traceId(), finalCmd.resolution());
                                redisService.setValues(redisKey, "RETRYING (1/5)");
                                delivery.nack(false);
                                return Mono.empty(); // 스트림 유지
                            });

                }, concurrency)
                .subscribe();

    }


//    // 주기적인 모니터링 작업을 수행. 실패 시 재시도 큐로 보냄.
//    @RabbitListener(id = "Monitoring_worker",queues = RabbitNames.Q_WORK, concurrency = "5", containerFactory="probeContainerFactory"
//    )
//    void receiveMessage(CheckMediaManifestCmd cmd, Message message, Channel channel){
//        log.info("모니터링 작업 수신. TraceId: {}, Resolution: {}", cmd.traceId(), cmd.resolution());
//
//        String lockKey = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());
//        Duration ttl = Duration.ofMinutes(3L); // 워커가 비정상 종료될 경우를 대비한 TTL
//        boolean lockAcquired = redisService.getOpsAbsent(lockKey, "1", ttl);
//
//        if (!lockAcquired) {
//            log.info("다른 워커가 이미 작업을 수행 중입니다. Key: {}", lockKey);
//            return; // Lock 획득에 실패하면 중복 작업이므로 즉시 종료
//        }
//
//        Instant nowTime = Instant.now();
//        log.info("목표 시간과의 차이: {}ms", Duration.between(cmd.publishTime(), nowTime).toMillis());
//
//        // 1. 미디어 매니페스트 다운로드
//        getMediaService.getMediaNonBlocking(cmd.mediaUrl(), cmd.userAgent())
//                .map(media -> {
//                    Duration requestDuration = Duration.between(nowTime, Instant.now());
//                    // 2. 매니페스트 파싱
//                    return parseMediaManifest.parse(media, requestDuration, cmd.traceId(), cmd.resolution());
//                })
//                .flatMap(dto ->
//                    Mono.fromRunnable(() -> {
//                                // 3. Redis에 결과 저장
//                                addToRedis(cmd.traceId(), cmd.resolution(), dto);
//
//                                // 4. 다음 작업 스케줄링
//                                sendDelay(cmd);
//                                log.info("작업 처리 완료. 처리 시간: {}ms", Duration.between(nowTime, Instant.now()).toMillis());
//                            })
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .thenReturn(dto)
//                )
//                .doOnSuccess(dto -> {
//                    // RabbitMQ에 정상 처리 완료 통보
//                    try {
//                        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .doOnError(e -> {
//                    log.error("모니터링 작업 실패. 재시도 큐로 보냅니다. TraceId: {}, Resolution: {}, Error: {}", cmd.traceId(), cmd.resolution(), e.getMessage());
//
//                    // 재시도 상태(1/5)를 Redis에 기록
//                    String redisKey = RedisKeys.state(cmd.traceId(), cmd.resolution());
//                    redisService.setValues(redisKey, "RETRYING (1/5)");
//
//                    // 실패 시 DLX로 (requeue=false)
//                        try {
//                            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
//                        } catch (IOException ex) {
//                            throw new RuntimeException(ex);
//                        }
//                    }
//                )
//                .doFinally(signal -> {
//                    redisService.deleteValues(lockKey);
//                    log.debug("작업 완료 후 Lock을 해제합니다. Key: {}", lockKey);
//                })
//                .subscribe();
//    }
}