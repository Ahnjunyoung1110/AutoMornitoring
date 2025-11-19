package AutoMonitoring.AutoMonitoring.domain.program.application;

import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.SaveM3u8OptionCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramOptionCommand;
import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramAlreadyExistException;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramNotFoundException;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepo programRepo;


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
}
