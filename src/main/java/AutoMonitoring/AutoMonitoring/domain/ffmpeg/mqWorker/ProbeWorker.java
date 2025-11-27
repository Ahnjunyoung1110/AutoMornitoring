package AutoMonitoring.AutoMonitoring.domain.ffmpeg.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.FfmpegCommand;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.ProbeCommand;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.RefreshCommand;
import AutoMonitoring.AutoMonitoring.contract.program.DbCreateProbeCommand;
import AutoMonitoring.AutoMonitoring.contract.program.DbRefreshProbeCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProbeDTO;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter.MediaProbe;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramNotFoundException;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProbeWorker {

    private final MediaProbe mediaProbe;
    private final RabbitTemplate rabbit;
    private final RedisService redisService;

    @RabbitListener(queues = RabbitNames.Q_STAGE1,
            concurrency = "3")
    public void handle(FfmpegCommand cmd){
        ProbeDTO responseDTO;

        switch (cmd){

            // 처음 프로그램을 입력한 경우
            case ProbeCommand c -> {
                Optional<ProbeDTO> dto = get(c);
                if(dto.isEmpty()) return;

                log.info(String.valueOf(dto));
                DbCreateProbeCommand newCmd = new DbCreateProbeCommand(cmd.traceId(), dto.get());

                rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE2, newCmd);
            }

            // 동일한 master url 로 sub를 갱신하는 경우
            case RefreshCommand c -> {
                try{
                    Optional<ProbeDTO> dto = get(c);
                    if(dto.isEmpty()) return;

                    log.info(String.valueOf(dto));
                    DbRefreshProbeCommand newCmd = new DbRefreshProbeCommand(cmd.traceId(), dto.get());

                    rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE2, newCmd);
                } catch (ProgramNotFoundException e) {
                    log.warn("Program not found. 드랍합니다. traceId={}", c.traceId(), e);
                }
            }
        }



    }

    private Optional<ProbeDTO> get(FfmpegCommand cmd){
        try {
            log.info("Probing master URL: {}", cmd.masterUrl());
            ProbeDTO responseDTO = mediaProbe.probe(cmd);
            log.info("ffprobe로 정보 가져오기 완료");
            return Optional.of(responseDTO);
        } catch (Exception e) {
            log.error("Failed to probe master URL. TraceId: {}, Error: {}", cmd.traceId(), e.getMessage());
            redisService.setValues(cmd.traceId(), "PROBE_FAILED");
            return Optional.empty();
        }

    }
}
