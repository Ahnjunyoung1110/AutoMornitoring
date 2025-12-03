package AutoMonitoring.AutoMonitoring.contract.program;

public record ProgramStatusCommand (
        String traceId,
        String resolution,
        ResolutionStatus status
)implements ProgramCommand{
}
