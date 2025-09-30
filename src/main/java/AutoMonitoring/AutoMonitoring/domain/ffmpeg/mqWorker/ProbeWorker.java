package AutoMonitoring.AutoMonitoring.domain.ffmpeg.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter.MediaProbe;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.dto.ProbeCommand;
import AutoMonitoring.AutoMonitoring.domain.program.dto.DbCommand;
import AutoMonitoring.AutoMonitoring.domain.program.dto.ProbeDTO;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProbeWorker {

    private final MediaProbe mediaProbe;
    private final RabbitTemplate rabbit;
    private final RedisService redisService;

    @RabbitListener(queues = RabbitNames.Q_STAGE1)
    public void handle(ProbeCommand cmd){
        ProbeDTO responseDTO;
        try {
            log.info("Probing master URL: {}", cmd.masterUrl());
            responseDTO = mediaProbe.probe(cmd);
            log.info("ffprobe로 정보 가져오기 완료");
        } catch (Exception e) {
            log.error("Failed to probe master URL. TraceId: {}, Error: {}", cmd.traceId(), e.getMessage());
            redisService.setValues(cmd.traceId(), "PROBE_FAILED");
            return; // Stop processing
        }

        log.info(String.valueOf(responseDTO));
        DbCommand newCmd = new DbCommand(cmd.traceId(), responseDTO);

        rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE2, newCmd);
    }
}
