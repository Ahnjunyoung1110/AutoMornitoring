package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramStatusCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception.SessionExpiredException;
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
            ProgramStatusCommand statusCmd = new ProgramStatusCommand(info.getTraceId(), key, ResolutionStatus.MONITORING);

            rabbitTemplate.convertAndSend(RabbitNames.EX_PROGRAM_COMMAND, RabbitNames.RK_PROGRAM_COMMAND, statusCmd);
            StartMonitoringDTO dto = new StartMonitoringDTO(info.getTraceId(), info.getResolutionToUrl().get(key) , key, info.getUserAgent(), epoch);
            monitoringService.startMornitoring(dto);
        }
        log.info("모니터링을 시작합니다. TraceId: {}, 대상 해상도: {}", info.getTraceId(), info.getResolutionToUrl().keySet());
    }


    @PostConstruct
    public void startMonitoring(){
        int prefetch = 52;
        int concurrency = 80;
        log.info("PostConstruct 가 실행되었습니다.");

        receiver.consumeManualAck(RabbitNames.Q_WORK, new ConsumeOptions().qos(prefetch))
                .flatMap(delivery -> {
                    byte[] body = delivery.getBody();
                    CheckMediaManifestCmd cmd = null;
                    try {
                        cmd = objectMapper.readValue(delivery.getBody(), CheckMediaManifestCmd.class);
                        log.info("{}", cmd);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    CheckMediaManifestCmd finalCmd = cmd;
                    return handler.handle(cmd)
                            .doOnSuccess(__ -> delivery.ack())
                            .onErrorResume(e -> {

                                // 세션 만료의 경우 재시도 X
                                if(e instanceof SessionExpiredException se) return Mono.empty();


                                // 이외 실패의 경우
                                log.error("모니터링 작업 실패. 재시도 큐로 보냅니다. TraceId: {}, Resolution: {}, Error: {}", finalCmd.traceId(), finalCmd.resolution(), e.getMessage());

                                // 재시도 상태를 DB에 기록
                                rabbitTemplate.convertAndSend(RabbitNames.EX_PROGRAM_COMMAND, RabbitNames.EX_PROGRAM_COMMAND, ResolutionStatus.RETRYING);
                                delivery.nack(false);
                                return Mono.empty(); // 스트림 유지
                            });

                }, concurrency)
                .subscribe();

    }
}