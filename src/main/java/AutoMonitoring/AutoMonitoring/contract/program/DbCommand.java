package AutoMonitoring.AutoMonitoring.contract.program;


public sealed interface DbCommand
        permits DbCreateCommand, DbRefreshCommand {
    String traceId();
    ProbeDTO probeDTO();
}
