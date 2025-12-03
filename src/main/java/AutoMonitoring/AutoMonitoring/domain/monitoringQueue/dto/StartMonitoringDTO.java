package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto;

public record StartMonitoringDTO(String traceId, String manifestUrl, String resolution, String userAgent, long epoch) {
}
