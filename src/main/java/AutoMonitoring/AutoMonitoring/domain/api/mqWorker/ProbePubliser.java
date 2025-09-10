package AutoMonitoring.AutoMonitoring.domain.api.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitConfig;
import AutoMonitoring.AutoMonitoring.domain.dto.ProbeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProbePubliser {
    private final RabbitTemplate rabbit;

    public void publish(ProbeCommand cmd){
        rabbit.convertAndSend("ex.pipeline", "route.stage2", cmd);
    }
}
