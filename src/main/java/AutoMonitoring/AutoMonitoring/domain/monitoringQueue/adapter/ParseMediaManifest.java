package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;

public interface ParseMediaManifest {
    RecordMediaToRedisDTO parse(String manifest);
}
