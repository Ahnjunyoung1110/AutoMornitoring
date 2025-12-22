package AutoMonitoring.AutoMonitoring.contract.monitoringQueue;

import java.time.Instant;

public record CheckMediaManifestCmd(String mediaUrl,
                                    String resolution,
                                    String userAgent,
                                    Integer bandWidth,
                                    Integer failCount,
                                    Instant publishTime,
                                    String traceId,
                                    Long epoch
) {

    public CheckMediaManifestCmd(String mediaUrl,
                                 String resolution,
                                 String userAgent,
                                 Integer failCount,
                                 Instant publishTime,
                                 String traceId,
                                 Long epoch){
        this(mediaUrl, resolution, userAgent, null, failCount, publishTime, traceId, epoch);
    }
}
