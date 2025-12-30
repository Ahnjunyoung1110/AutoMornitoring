package AutoMonitoring.AutoMonitoring.domain.api.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.ProbeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProbePublisher {
    private final RabbitTemplate rabbit;

    public void publish(ProbeCommand cmd){

        rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_FFMPEG, cmd);
    }
}
