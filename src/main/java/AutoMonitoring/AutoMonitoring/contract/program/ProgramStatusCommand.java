package AutoMonitoring.AutoMonitoring.contract.program;

public record ProgramStatusCommand (
        String traceId,
        String resolution,
        ResolutionStatus status,
        Integer bandWidth
)implements ProgramCommand{
    // bandWidth 없는 버전 (null로 세팅)
    public ProgramStatusCommand(String traceId, String resolution, ResolutionStatus status) {
        this(traceId, resolution, status, null);
    }
}
