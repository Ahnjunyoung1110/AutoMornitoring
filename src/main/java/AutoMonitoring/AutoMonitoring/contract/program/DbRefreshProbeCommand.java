package AutoMonitoring.AutoMonitoring.contract.program;

public record DbRefreshProbeCommand(String traceId, ProbeDTO probeDTO)
        implements DbProbeCommand {
}
