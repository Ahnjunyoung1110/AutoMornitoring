package AutoMonitoring.AutoMonitoring.contract.program;

public sealed interface DbCommand
        permits DbGetCommand, DbProbeCommand, LogValidationFailureCommand {
}
