package AutoMonitoring.AutoMonitoring.contract.program;

import AutoMonitoring.AutoMonitoring.contract.Command;

public sealed interface DbCommand extends Command
        permits DbGetCommand, DbProbeCommand, LogValidationFailureCommand {
}
