package AutoMonitoring.AutoMonitoring.domain.api.adapter;


public interface RecordManifest {
    String recordMasterManifest(String MasterManifestUrl, String UserAgent);
    void refreshMonitoring(String traceId);
}
