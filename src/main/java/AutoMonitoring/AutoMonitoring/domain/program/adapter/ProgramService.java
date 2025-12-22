package AutoMonitoring.AutoMonitoring.domain.program.adapter;

import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.SaveM3u8OptionCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.StopMonitoringMQCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramInformation;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramOptionCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramStatusCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramStopCommand;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;

import java.util.List;
import java.util.Map;

public interface ProgramService {
    Program saveProgram(Program program);
    Program get(String id);

    Program getByTraceId(String traceId);

    Program updateProgram(Program program);

    SaveM3u8OptionCommand setOption(ProgramOptionCommand command);
    void delete(String id);

    void setStatus(ProgramStatusCommand c);

    Map<String, List<ProgramInformation>> getInformationByTraceId(String traceId);

    StopMonitoringMQCommand stopMonitoring(ProgramStopCommand c);

    List<Program> getAllFailedPrograms();

    void updateSystemConfig(AutoMonitoring.AutoMonitoring.contract.program.UpdateSystemConfigCommand command);
}
