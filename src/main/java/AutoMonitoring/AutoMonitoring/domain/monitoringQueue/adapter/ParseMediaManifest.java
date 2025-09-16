package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;

import java.time.Duration;

public interface ParseMediaManifest {
    RecordMediaToRedisDTO parse(String manifest, Duration requestDurationMs, String traceId, String resolution);
}
