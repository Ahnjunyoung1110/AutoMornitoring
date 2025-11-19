package AutoMonitoring.AutoMonitoring.domain.program.mqworker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.RefreshCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.MonitoringCommand;
import AutoMonitoring.AutoMonitoring.contract.program.*;
import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbWorker {
    private final ProgramService programService;
    private final RedisService redis;
    private final RabbitTemplate rabbit;

    // insert
    @RabbitListener(
            queues = RabbitNames.Q_STAGE2,
            errorHandler = "globalRabbitErrorHandler")
    public void handle(DbCommand cmd){
        // db 에 저장
        Program program = Program.fromDto(cmd.probeDTO());

        switch(cmd){
            // 최초 생성
            case DbCreateCommand c -> {
                Program savedProgram = programService.saveProgram(program);
                redis.setValues(cmd.traceId(), "MONITORING");

                // 이후 모니터링을 시행
                ProgramInfo stream = ProgramInfo.getProgramInfo(savedProgram);
                rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE3, stream);
            }

            // submenifest 갱신
            case DbRefreshCommand c -> {
                Program savedProgram = programService.updateProgram(program);
                redis.setValues(cmd.traceId(), "MONITORING");

                // 이후 모니터링을 시행
                ProgramInfo stream = ProgramInfo.getProgramInfo(savedProgram);
                rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE3, stream);
            }
        }







    }

    // Program DB 와의 상호작용 큐
    @RabbitListener(
            queues = RabbitNames.Q_PROGRAM_COMMAND,
            errorHandler = "globalRabbitErrorHandler")
    public void handleCommand(ProgramCommand command){
        switch (command){
            // 프로그램 m3u8 저장 옵션 변경
            case ProgramOptionCommand c -> {
                MonitoringCommand monitoringCommand = programService.setOption(c);
                rabbit.convertAndSend(RabbitNames.EX_MONITORING_COMMAND, RabbitNames.RK_MONITORING_COMMAND, monitoringCommand);
            }
            // 같은 마스터 URL, UserAgent로 서브매니페스트 url 갱신
            case ProgramRefreshRequestCommand c -> {
                Program findedProgram = programService.getByTraceId(c.traceId());
                RefreshCommand probeCommand = new RefreshCommand(findedProgram.getTraceId(), findedProgram.getMasterManifestUrl(), findedProgram.getUserAgent());
                rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE1, probeCommand);
            }
        }
    }

}
