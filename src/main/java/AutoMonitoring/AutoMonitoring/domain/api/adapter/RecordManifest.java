package AutoMonitoring.AutoMonitoring.domain.api.adapter;


public interface RecordManifest {
    String recordMasterManifest(String MasterManifestUrl, String UserAgent);
    void refreshMonitoring(String traceId);

    // 모니터링을 멈춘다
    void stopMonitoring(String traceId);
}
