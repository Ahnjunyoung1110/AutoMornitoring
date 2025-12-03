package AutoMonitoring.AutoMonitoring.contract.program;


public sealed interface DbProbeCommand extends DbCommand
        permits DbCreateProbeCommand, DbRefreshProbeCommand {
    String traceId();
    ProbeDTO probeDTO();
}
