package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto;

import java.time.Instant;

public record CheckMediaManifestCmd(String mediaUrl,
                                    Integer failCount,
                                    Instant publishTime,
                                    String traceId) {
}
