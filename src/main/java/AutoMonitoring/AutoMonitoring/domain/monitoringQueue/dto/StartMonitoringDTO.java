package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto;

public record StartMonitoringDTO(String traceId, String manifestUrl, String resolution, Integer bandWidth,String userAgent, long epoch) {
}
