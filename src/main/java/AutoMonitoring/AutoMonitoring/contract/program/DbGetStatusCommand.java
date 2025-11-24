package AutoMonitoring.AutoMonitoring.contract.program;

public record DbGetStatusCommand(
        String traceId
) implements DbGetCommand {
}
