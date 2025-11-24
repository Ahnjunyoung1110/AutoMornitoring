package AutoMonitoring.AutoMonitoring.contract.program;

public sealed interface DbGetCommand extends DbCommand
        permits DbSummaryCommand, DbGetStatusCommand{
}
