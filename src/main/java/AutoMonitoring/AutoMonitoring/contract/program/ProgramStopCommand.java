package AutoMonitoring.AutoMonitoring.contract.program;

public record ProgramStopCommand(
        String traceId
) implements ProgramCommand
        {
}
