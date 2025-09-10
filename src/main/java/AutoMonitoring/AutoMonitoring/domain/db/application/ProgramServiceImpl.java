package AutoMonitoring.AutoMonitoring.domain.db.application;

import AutoMonitoring.AutoMonitoring.domain.db.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.db.exception.ProgramAlreadyExistException;
import AutoMonitoring.AutoMonitoring.domain.db.exception.ProgramNotFoundException;
import AutoMonitoring.AutoMonitoring.domain.db.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.db.repository.ProgramRepo;
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
        try {
            return programRepo.save(program);
        }
        // 해당 마스터 메니페스트가 이미 존재하는경우
        catch (DataIntegrityViolationException e) {
            Program existsProgram = findByMasterManifestUrl(program.getMasterManifestUrl());
            throw new ProgramAlreadyExistException("해당 프로그램이 이미 존재합니다. 프로그램 정보: " + existsProgram);
        }

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
