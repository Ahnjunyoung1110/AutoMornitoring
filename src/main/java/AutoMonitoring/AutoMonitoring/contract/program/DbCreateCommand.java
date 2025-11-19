package AutoMonitoring.AutoMonitoring.contract.program;

public record DbCreateCommand(String traceId, ProbeDTO probeDTO)
implements DbCommand{
}
