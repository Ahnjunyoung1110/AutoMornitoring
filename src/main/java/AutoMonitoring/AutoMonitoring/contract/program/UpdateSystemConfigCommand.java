package AutoMonitoring.AutoMonitoring.contract.program;

import AutoMonitoring.AutoMonitoring.contract.Command;
import lombok.Builder;

@Builder
public record UpdateSystemConfigCommand(
        boolean alarmEnabled,
        int threshold,
        int alarmCooldownSeconds,
        int reconnectThreshold,
        int reconnectTimeoutMillis,
        int reconnectRetryDelayMillis,
        int httpRequestTimeoutMillis,
        boolean autoRefresh,
        boolean monitoringEnabled
) implements Command {
}
