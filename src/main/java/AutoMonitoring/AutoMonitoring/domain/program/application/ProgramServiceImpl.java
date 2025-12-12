package AutoMonitoring.AutoMonitoring.domain.program.application;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.UpdateAlarmConfigCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.QueueSystemConfigCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.SaveM3u8OptionCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.StopMonitoringMQCommand;
import AutoMonitoring.AutoMonitoring.contract.program.*;
import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.SystemConfig;
import AutoMonitoring.AutoMonitoring.domain.program.entity.VariantInfoEmb;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramAlreadyExistException;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramNotFoundException;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import AutoMonitoring.AutoMonitoring.domain.program.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepo programRepo;
    private final SystemConfigRepository systemConfigRepository;
    private final RabbitTemplate rabbit;


    @Override
    public Program saveProgram(Program program) {
        Optional<Program> existingProgram = programRepo.findByTraceId(program.getTraceId());
        if(existingProgram.isPresent()) throw new ProgramAlreadyExistException("해당 TraceId가 이미 존재합니다.");
        return programRepo.save(program);
    }


    @Override
    public Program get(String id) {
        Optional<Program> findByIdProgram = programRepo.findById(id);
        if (findByIdProgram.isEmpty()) throw new ProgramNotFoundException("해당 id의 프로그램이 존재하지 않습니다.");
        return findByIdProgram.get();
    }

    @Override
    public Program getByTraceId(String traceId){
        Optional<Program> findByTraceIdProgram = programRepo.findByTraceId(traceId);
        if (findByTraceIdProgram.isEmpty()) throw new ProgramNotFoundException("해당 TraceId의 프로그램이 존재하지 않습니다.");
        return findByTraceIdProgram.get();
    }


    @Transactional
    @Override
    public Program updateProgram(Program program) {
        Optional<Program> findByTraceIdProgram = programRepo.findByTraceId(program.getTraceId());
        if (findByTraceIdProgram.isEmpty()) throw new ProgramNotFoundException("해당 TraceId의 프로그램이 존재하지 않습니다.");
        Program updateProgram = findByTraceIdProgram.get();

        log.info("프로그램을 업데이트 합니다 {}",program);
        updateProgram.update(program);

        return updateProgram;
    }

    @Transactional
    @Override
    public SaveM3u8OptionCommand setOption(ProgramOptionCommand command) {
        Program program = programRepo.findByTraceId(command.traceId())
                .orElseThrow(() -> new ProgramNotFoundException("해당 traceId의 프로그램이 존재하지 않습니다."));

        program.applyOption(command);
        log.info("프로그램 옵션을 변경했습니다. TraceId: {} , saveM3u8State: {}", program.getTraceId(), program.getSaveM3u8State());

        return new SaveM3u8OptionCommand(program.getTraceId(), command.saveM3u8State());
    }

    @Override
    public void delete(String id) {
//        Optional<Program> findedProgram = programRepo.findById(id);
//        if (findedProgram.isEmpty()) throw new ProgramNotFoundException("해당 id의 프로그램이 존재하지 않습니다.");
//
//        programRepo.delete(findedProgram.get());
    }

    @Transactional
    @Override
    public void setStatus(ProgramStatusCommand c) {
        Program program = programRepo.findByTraceId(c.traceId())
                .orElseThrow(() -> new ProgramNotFoundException("프로그램이 존재하지 않습니다."));

        VariantInfoEmb variantInfoEmb = program.findVariantByResolution(c.resolution())
                .orElseThrow(() -> new ProgramNotFoundException("해당 resolution의 프로그램이 존재하지 않습니다."));

        variantInfoEmb.changeStatus(c.status());

    }

    @Override
    public Map<String, String> getStatuesByTraceId(String traceId) {
        List<VariantInfoEmb> variants = programRepo.findVarient(traceId).orElseThrow(
                () -> new ProgramNotFoundException("프로그램이 존재하지 않습니다.")
        );

        Map<String, String> statusMap = new HashMap<>();
        for (VariantInfoEmb emb : variants){
            statusMap.put(emb.getResolution(), emb.getStatus().name());
        }

        log.info("접근 완료");
        return statusMap;
    }

    @Override
    @Transactional
    public StopMonitoringMQCommand stopMonitoring(ProgramStopCommand c) {
        Program program = programRepo.findByTraceId(c.traceId())
                .orElseThrow(() -> new ProgramNotFoundException("프로그램이 존재하지 않습니다."));

        // db 상의 상태 변경
        for(VariantInfoEmb emb : program.getVariants()){
            emb.changeStatus(ResolutionStatus.STOP);
        }

        // MQ 에서 모니터링을 중지하도록 변경
        StopMonitoringMQCommand command = new StopMonitoringMQCommand(c.traceId());
        return command;
    }

    @Override
    public List<Program> getAllFailedPrograms() {
        return programRepo.findAllByVariantStatusFailed();
    }

    @Transactional
    @Override
    public void updateSystemConfig(UpdateSystemConfigCommand command) {
        // ID 1번으로 항상 고정하여 사용
        SystemConfig systemConfig = new SystemConfig(
                1L,
                command.alarmEnabled(),
                command.threshold(),
                command.alarmCooldownSeconds(),
                command.reconnectThreshold(),
                command.reconnectTimeoutMillis(),
                command.reconnectRetryDelayMillis(),
                command.httpRequestTimeoutMillis(),
                command.autoRefresh(),
                command.monitoringEnabled()
        );

        systemConfigRepository.save(systemConfig);
        log.info("시스템 설정을 DB에 업데이트했습니다.");

        // 다른 도메인에 전파할 Command 생성
        QueueSystemConfigCommand notificationCommand = new QueueSystemConfigCommand(
                command.reconnectThreshold(),
                command.reconnectTimeoutMillis(),
                command.reconnectRetryDelayMillis(),
                command.httpRequestTimeoutMillis(),
                command.autoRefresh(),
                command.monitoringEnabled()
        );

        // monitoringQueue 도메인으로 설정 전파
        rabbit.convertAndSend(RabbitNames.EX_MONITORING_COMMAND, RabbitNames.RK_MONITORING_COMMAND, notificationCommand);
        log.info("monitoringQueue 도메인으로 설정 변경을 전파했습니다.");

        // checkValid 도메인으로 알람 관련 설정만 전파
        UpdateAlarmConfigCommand alarmConfigCommand = new UpdateAlarmConfigCommand(
                command.alarmEnabled(),
                command.threshold(),
                command.alarmCooldownSeconds()
        );
        rabbit.convertAndSend(RabbitNames.EX_CHECKVALID_COMMAND, RabbitNames.RK_CHECKVALID_COMMAND, alarmConfigCommand);
        log.info("checkValid 도메인으로 알람 설정 변경을 전파했습니다.");
    }
}
