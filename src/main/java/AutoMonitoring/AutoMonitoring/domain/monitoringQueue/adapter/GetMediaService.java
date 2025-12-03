package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import reactor.core.publisher.Mono;

public interface GetMediaService {
    String getMedia(String url, String userAgent, String traceId);

    Mono<String> getMediaNonBlocking(String url, String userAgent, String traceId);
}
