package AutoMonitoring.AutoMonitoring.contract.program;

public sealed interface  ProgramCommand
        permits ProgramOptionCommand, ProgramRefreshRequestCommand {
    String traceId();
}
