package AutoMonitoring.AutoMonitoring.contract.program;

import AutoMonitoring.AutoMonitoring.contract.Command;

public sealed interface  ProgramCommand extends Command
        permits ProgramOptionCommand, ProgramRefreshRequestCommand, ProgramStatusCommand, ProgramStopCommand {
    String traceId();
}