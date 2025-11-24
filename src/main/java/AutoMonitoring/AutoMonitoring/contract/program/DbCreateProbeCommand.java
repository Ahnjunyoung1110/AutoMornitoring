package AutoMonitoring.AutoMonitoring.contract.program;

public record DbCreateProbeCommand(String traceId, ProbeDTO probeDTO)
implements DbProbeCommand {
}
