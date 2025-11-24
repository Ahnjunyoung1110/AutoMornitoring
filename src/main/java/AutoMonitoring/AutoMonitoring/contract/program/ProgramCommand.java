package AutoMonitoring.AutoMonitoring.contract.program;

public sealed interface  ProgramCommand
        permits ProgramOptionCommand, ProgramRefreshRequestCommand, ProgramStatusCommand {
    String traceId();
}
