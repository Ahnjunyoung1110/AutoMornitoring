package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramStatusCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import AutoMonitoring.AutoMonitoring.contract.program.VariantDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception.SessionExpiredException;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util.MonitoringConfigHolder;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util.MonitoringJobHandler;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class MonitoringWorker {
    private final RedisService redisService;
    private final MonitoringService monitoringService;
    private final MonitoringConfigHolder monitoringConfigHolder;

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
        Map<String, List<VariantDTO>> resolutionToUrls = info.getVariantsByResolution();

        for(Map.Entry<String, List<VariantDTO>> entry : resolutionToUrls.entrySet()) {
            String resolution = entry.getKey();
            List<VariantDTO> variants = entry.getValue();

            for(VariantDTO variant : variants) {

                ProgramStatusCommand statusCmd = new ProgramStatusCommand(
                        info.getTraceId(), resolution, ResolutionStatus.MONITORING, variant.bandwidth()
                );
                rabbitTemplate.convertAndSend(
                        RabbitNames.EX_PROGRAM_COMMAND,
                        RabbitNames.RK_PROGRAM_COMMAND,
                        statusCmd
                );

                StartMonitoringDTO dto = new StartMonitoringDTO(
                        info.getTraceId(), variant.uri(), resolution, variant.bandwidth(),info.getUserAgent(), epoch
                );
                monitoringService.startMornitoring(dto);
            }
        }
        log.info("모니터링을 시작합니다. TraceId: {}, 대상 해상도: {}", info.getVariantsByResolution().keySet());
    }


    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring(ApplicationReadyEvent event){
        int prefetch = 80;
        int concurrency = 80;
        log.info("애플리케이션 준비 완료, 'working.queue.stage1' 큐에 대한 소비자(리스너)를 시작합니다.");

        receiver.consumeManualAck(RabbitNames.Q_WORK, new ConsumeOptions().qos(prefetch))
                .flatMap(delivery -> {
                    CheckMediaManifestCmd cmd = null;
                    try {
                        cmd = objectMapper.readValue(delivery.getBody(), CheckMediaManifestCmd.class);
                    } catch (IOException e) {
                        delivery.ack(); // 메시지 파싱 실패시 복구 불가능하므로 버림
                        log.error("메시지 파싱 실패. 해당 메시지를 버립니다.", e);
                        return Mono.empty();
                    }

                    // 모니터링 비활성화 시 메시지 일시정지
                    if (!monitoringConfigHolder.getMonitoringEnabled().get()) {
                        log.warn("모니터링이 비활성화 되어있습니다. 4초 후 다시 시도합니다. TraceId: {}", cmd.traceId());

                        final CheckMediaManifestCmd snapshotCmd = cmd;
                        // 전송과 ACK를 원자적으로 처리
                        return Mono.fromRunnable(() -> {
                            rabbitTemplate.convertAndSend(RabbitNames.EX_DELAY, RabbitNames.RK_DELAY_4S, snapshotCmd, msg -> {
                                msg.getMessageProperties().setExpiration("4000");
                                return msg;
                            });
                        }).doOnSuccess(v -> delivery.ack()).then();
                    }

                    log.info("메시지를 수신 받았습니다: {}", cmd);
                    CheckMediaManifestCmd finalCmd = cmd;
                    return handler.handle(cmd)
                            .doOnSuccess(__ -> delivery.ack())
                            .onErrorResume(e -> {
                                delivery.nack(false);
                                // 세션 만료의 경우 재시도 X
                                if(e instanceof SessionExpiredException se) return Mono.empty();


                                // 이외 실패의 경우
                                log.warn("모니터링 작업 실패. 재시도 큐로 보냅니다. TraceId: {}, Resolution: {}, Error: {}", finalCmd.traceId(), finalCmd.resolution(), e.getMessage());

                                // 재시도 상태를 DB에 기록
                                ProgramStatusCommand statusCmd = new ProgramStatusCommand(
                                        finalCmd.traceId(), finalCmd.resolution(), ResolutionStatus.RETRYING, finalCmd.bandWidth()
                                );
                                rabbitTemplate.convertAndSend(
                                        RabbitNames.EX_PROGRAM_COMMAND,
                                        RabbitNames.RK_PROGRAM_COMMAND,
                                        statusCmd
                                );
                                rabbitTemplate.convertAndSend(RabbitNames.EX_PROGRAM_COMMAND, RabbitNames.EX_PROGRAM_COMMAND, statusCmd);
                                return Mono.empty(); // 스트림 유지
                            });

                }, concurrency)
                .subscribe();

    }
}