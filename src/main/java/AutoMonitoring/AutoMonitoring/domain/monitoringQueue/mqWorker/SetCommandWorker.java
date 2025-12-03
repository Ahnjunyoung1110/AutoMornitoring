package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.MonitoringCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.SaveM3u8OptionCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.StopMonitoringMQCommand;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.SetOptionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class SetCommandWorker {


    private final RabbitTemplate rabbit;
    private final SetOptionsService setOptionsService;
    private final MonitoringService monitoringService;

    // Program DB 와의 상호작용 큐
    @RabbitListener(queues = RabbitNames.Q_MONITORING_COMMAND)
    public void handleCommand(MonitoringCommand command){
        switch (command){
            case SaveM3u8OptionCommand c -> {
                setOptionsService.setOptions(c);
                log.info("성공적으로 저장 옵션을 변경하였습니다. traceId: {} Option: {}", c.traceId(), c.saveM3u8State());
            }

            case StopMonitoringMQCommand c -> {
                monitoringService.stopMornitoring(c);
                log.info("성공적으로 프로그램 모니터링을 종료하였습니다. traceId: {}", c.traceId());
            }
        }
    }
}
