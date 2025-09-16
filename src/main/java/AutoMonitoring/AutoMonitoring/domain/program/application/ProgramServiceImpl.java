package AutoMonitoring.AutoMonitoring.domain.program.application;

import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramAlreadyExistException;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramNotFoundException;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepo programRepo;


    @Override
    public Program saveProgram(Program program) {
        Optional<Program> existingProgram = programRepo.findByTraceId(program.getTraceId());
        if(existingProgram.isPresent()) throw new ProgramAlreadyExistException("해당 TraceId가 이미 존재합니다.");
        return programRepo.save(program);

    }

    private final Program findByMasterManifestUrl(String MasterUrl){
        return programRepo.findByMasterManifestUrl(MasterUrl);
    }

    @Override
    public Program get(String id) {
        Optional<Program> findByIdProgram = programRepo.findById(id);
        if (findByIdProgram.isEmpty()) throw new ProgramNotFoundException("해당 id의 프로그램이 존재하지 않습니다.");
        return findByIdProgram.get();
    }

    @Override
    public Program update(String id, Map<String, Object> updateData) {
        return null;
//        Optional<Program> findedProgram = programRepo.findById(id);
//        if (findedProgram.isEmpty()) throw new ProgramNotFoundException("해당 id의 프로그램이 존재하지 않습니다.");
//
//        findedProgram.get().updateChanges(updateData);
//        return programRepo.save(findedProgram.get());
    }

    @Override
    public void delete(String id) {
//        Optional<Program> findedProgram = programRepo.findById(id);
//        if (findedProgram.isEmpty()) throw new ProgramNotFoundException("해당 id의 프로그램이 존재하지 않습니다.");
//
//        programRepo.delete(findedProgram.get());
    }
}
