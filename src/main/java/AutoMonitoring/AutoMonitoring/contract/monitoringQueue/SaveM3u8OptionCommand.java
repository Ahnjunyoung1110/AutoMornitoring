package AutoMonitoring.AutoMonitoring.contract.monitoringQueue;

import AutoMonitoring.AutoMonitoring.contract.program.SaveM3u8State;
import jakarta.validation.constraints.NotBlank;

public record SaveM3u8OptionCommand(
@NotBlank
String traceId,
SaveM3u8State saveM3u8State
) implements MonitoringCommand {
}
