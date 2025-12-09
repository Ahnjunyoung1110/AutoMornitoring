package AutoMonitoring.AutoMonitoring.domain.api.dto;

import lombok.Builder;

@Builder
public record SystemConfigRequest(
        boolean alarmEnabled,
        int threshold,
        int alarmCooldownSeconds,
        int reconnectThreshold,
        int reconnectTimeoutMillis,
        int reconnectRetryDelayMillis,
        int httpRequestTimeoutMillis,
        boolean autoRefresh,
        boolean monitoringEnabled
) {
}
