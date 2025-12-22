package AutoMonitoring.AutoMonitoring.domain.program.mqworker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.Command;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.RefreshCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.MonitoringCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.StopMonitoringMQCommand;
import AutoMonitoring.AutoMonitoring.contract.program.*;
import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.program.application.DashBoardService;
import AutoMonitoring.AutoMonitoring.domain.program.application.ValidationLogService;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbWorker {
    private final ProgramService programService;
    private final ValidationLogService validationLogService;
    private final RabbitTemplate rabbit;
    private final DashBoardService dashBoardService;

    // DB에 대한 조회 또는 변경
    @RabbitListener(
            id = "DB_CHANGE",
            queues = RabbitNames.Q_STAGE2,
            errorHandler = "globalRabbitErrorHandler")
    public Object handle(DbCommand cmd){


        switch(cmd){
            // 최초 생성
            case DbCreateProbeCommand c -> {
                // db 에 저장
                Program program = Program.fromDto(c.probeDTO());

                Program savedProgram = programService.saveProgram(program);

                // 이후 모니터링을 시행
                ProgramInfo stream = ProgramInfo.getProgramInfo(savedProgram);
                rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE3, stream);
            }

            // submenifest 갱신
            case DbRefreshProbeCommand c -> {
                // db 에 저장
                Program program = Program.fromDto(c.probeDTO());

                Program savedProgram = programService.updateProgram(program);

                // 이후 모니터링을 시행
                ProgramInfo stream = ProgramInfo.getProgramInfo(savedProgram);
                rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE3, stream);
            }

            // M3U8 유효성 검사 실패 로그 저장
            case LogValidationFailureCommand c -> {
                try {
                    Program program = programService.getByTraceId(c.traceId());
                    validationLogService.saveValidationFailure(program, c.failureDTO());
                } catch (ProgramNotFoundException e) {
                    log.warn("유효성 검사 로그 저장 중 Program을 찾지 못했습니다. (traceId: {}). 로그 저장을 건너뜁니다.", c.traceId());
                }
            }

            // summary 생성 후 리턴
            case DbSummaryCommand c -> {
                // db에 summary 요청
//                DashBoardSummaryDTO dto =


            }
            // 조건에 맞는 모든 program 을 리턴
            case DbGetAllCommand c -> {
                return dashBoardService.getMonitoringPrograms(c.status(), c.traceId(), c.channelId(), c.tp(),  c.pageable());
            }

        }

        return null;
    }

    // Program DB 와의 상호작용 큐
    @RabbitListener(
            queues = RabbitNames.Q_PROGRAM_COMMAND,
            errorHandler = "globalRabbitErrorHandler")
    public void handleCommand(Command command){ // Changed type to Command
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

            // ProgramStatus 변경
            case ProgramStatusCommand c -> {
                programService.setStatus(c);
            }

            // 모니터링 중지
            case ProgramStopCommand c -> {
                StopMonitoringMQCommand command2 = programService.stopMonitoring(c);
                rabbit.convertAndSend(RabbitNames.EX_MONITORING_COMMAND, RabbitNames.RK_MONITORING_COMMAND, command2);
            }

            // 모든 실패한 프로그램을 새로 고침
            case ProgramRefreshAllFailedCommand c -> {
                List<Program> failedPrograms = programService.getAllFailedPrograms(); // This method needs to be added
                for (Program program : failedPrograms) {
                    ProgramRefreshRequestCommand refreshCmd = new ProgramRefreshRequestCommand(program.getTraceId());
                    rabbit.convertAndSend(RabbitNames.EX_PROGRAM_COMMAND, RabbitNames.RK_PROGRAM_COMMAND, refreshCmd);
                }
            }

            // 시스템 설정 업데이트
            case UpdateSystemConfigCommand c -> {
                programService.updateSystemConfig(c);
            }
            default -> throw new IllegalStateException("Unexpected value: " + command);
        }
    }

}