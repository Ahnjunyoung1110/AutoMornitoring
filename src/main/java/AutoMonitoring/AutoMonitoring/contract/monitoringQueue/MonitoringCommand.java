package AutoMonitoring.AutoMonitoring.contract.monitoringQueue;

import AutoMonitoring.AutoMonitoring.contract.Command;
import jakarta.validation.Valid;

public sealed interface MonitoringCommand extends Command
        permits SaveM3u8OptionCommand, StopMonitoringMQCommand {
    @Valid
    String traceId();
}
