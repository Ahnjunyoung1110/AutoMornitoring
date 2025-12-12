package AutoMonitoring.AutoMonitoring.contract.monitoringQueue;

import AutoMonitoring.AutoMonitoring.contract.Command;
import lombok.Builder;

@Builder
public record QueueSystemConfigCommand(
        int reconnectThreshold,
        int reconnectTimeoutMillis,
        int reconnectRetryDelayMillis,
        int httpRequestTimeoutMillis,
        boolean autoRefresh,
        boolean monitoringEnabled
) implements Command {}
