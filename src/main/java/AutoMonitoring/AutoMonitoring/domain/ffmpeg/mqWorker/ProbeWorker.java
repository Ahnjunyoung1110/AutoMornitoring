package AutoMonitoring.AutoMonitoring.domain.ffmpeg.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitConfig;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.dto.DbCommand;
import AutoMonitoring.AutoMonitoring.domain.dto.ProbeCommand;
import AutoMonitoring.AutoMonitoring.domain.dto.ProbeDTO;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter.MediaProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProbeWorker {

    private final MediaProbe mediaProbe;
    private final RabbitTemplate rabbit;

    @RabbitListener(queues = RabbitNames.Q_STAGE1)
    public void handle(ProbeCommand cmd){
        // stage1의 메시지를 소비한 후 stage2에 새로운 메시지를 발행하여 db에 저장 명령
        ProbeDTO responseDTO = mediaProbe.probe(cmd.masterUrl());

        DbCommand newCmd = new DbCommand(cmd.traceId(), responseDTO);

        rabbit.convertAndSend(RabbitNames.EX_PIPELINE, RabbitNames.Q_STAGE2, newCmd);


    }
}
