package AutoMonitoring.AutoMonitoring.contract.program;

public record DbRefreshCommand(String traceId, ProbeDTO probeDTO)
        implements DbCommand{
}
