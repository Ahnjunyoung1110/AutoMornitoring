package AutoMonitoring.AutoMonitoring.domain.api.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.dto.ProbeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProbePubliser {
    private final RabbitTemplate rabbit;

    public void publish(ProbeCommand cmd){

        rabbit.convertAndSend(RabbitNames.EX_PIPELINE, RabbitNames.RK_STAGE1, cmd);
    }
}
