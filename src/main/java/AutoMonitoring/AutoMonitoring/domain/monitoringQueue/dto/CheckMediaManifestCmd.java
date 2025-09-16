package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto;

import java.time.Instant;

public record CheckMediaManifestCmd(String mediaUrl,
                                    String resolution,
                                    String userAgent,
                                    Integer failCount,
                                    Instant publishTime,
                                    String traceId) {
}
