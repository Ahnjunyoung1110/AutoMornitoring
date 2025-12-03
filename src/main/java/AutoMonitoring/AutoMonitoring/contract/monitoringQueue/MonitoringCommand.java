package AutoMonitoring.AutoMonitoring.contract.monitoringQueue;

import jakarta.validation.Valid;

public sealed interface MonitoringCommand
        permits SaveM3u8OptionCommand, StopMonitoringMQCommand {
    @Valid
    String traceId();
}
