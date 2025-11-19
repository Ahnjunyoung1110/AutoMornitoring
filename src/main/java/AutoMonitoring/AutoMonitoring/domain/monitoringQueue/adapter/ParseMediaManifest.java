package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;

import java.time.Duration;

public interface ParseMediaManifest {
    CheckValidDTO parse(String manifest, Duration requestDurationMs, String traceId, String resolution);
}
