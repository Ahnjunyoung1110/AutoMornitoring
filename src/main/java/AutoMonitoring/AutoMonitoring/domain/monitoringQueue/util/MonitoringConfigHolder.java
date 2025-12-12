package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util;

import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.QueueSystemConfigCommand;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Getter
public class MonitoringConfigHolder {

    // Individual atomic fields for each configuration setting
    private final AtomicInteger reconnectThreshold;
    private final AtomicInteger reconnectTimeoutMillis;
    private final AtomicInteger reconnectRetryDelayMillis;
    private final AtomicInteger httpRequestTimeoutMillis;
    private final AtomicBoolean autoRefresh;
    private final AtomicBoolean monitoringEnabled;

    public MonitoringConfigHolder() {
        // Initialize with default values
        this.reconnectThreshold = new AtomicInteger(10);
        this.reconnectTimeoutMillis = new AtomicInteger(5000);
        this.reconnectRetryDelayMillis = new AtomicInteger(2000);
        this.httpRequestTimeoutMillis = new AtomicInteger(5000);
        this.autoRefresh = new AtomicBoolean(false);
        this.monitoringEnabled = new AtomicBoolean(true);
    }

    public void updateConfig(QueueSystemConfigCommand command) {
        this.reconnectThreshold.set(command.reconnectThreshold());
        this.reconnectTimeoutMillis.set(command.reconnectTimeoutMillis());
        this.reconnectRetryDelayMillis.set(command.reconnectRetryDelayMillis());
        this.httpRequestTimeoutMillis.set(command.httpRequestTimeoutMillis());
        this.autoRefresh.set(command.autoRefresh());
        this.monitoringEnabled.set(command.monitoringEnabled());
        System.out.println("System configuration updated via QueueSystemConfigCommand.");
    }
}