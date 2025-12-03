package AutoMonitoring.AutoMonitoring.contract.monitoringQueue;

import java.time.Instant;

public record CheckMediaManifestCmd(String mediaUrl,
                                    String resolution,
                                    String userAgent,
                                    Integer failCount,
                                    Instant publishTime,
                                    String traceId,
                                    Long epoch
) {
}
