package AutoMonitoring.AutoMonitoring.domain.program.adapter;

import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;

import java.util.Map;

public interface ProgramService {
    Program saveProgram(Program program);
    Program get(String id);
    Program update(String id, Map<String,Object> updateData);
    void delete(String id);
}
