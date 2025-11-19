package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
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

        String epochKey = RedisKeys.messageEpoch(info.getTraceId());
        long epoch = redisService.nextEpoch(epochKey);

        for(String key: info.getResolutionToUrl().keySet()){
            String redisKey = RedisKeys.state(info.getTraceId(), key);
            redisService.setValues(redisKey, "MONITORING");

            StartMonitoringDTO dto = new StartMonitoringDTO(info.getTraceId(), info.getResolutionToUrl().get(key) , key, info.getUserAgent(), epoch);
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
}