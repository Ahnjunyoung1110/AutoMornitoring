package AutoMonitoring.AutoMonitoring.contract.monitoringQueue;

public record StopMonitoringMQCommand(
        String traceId
) implements MonitoringCommand{
}
