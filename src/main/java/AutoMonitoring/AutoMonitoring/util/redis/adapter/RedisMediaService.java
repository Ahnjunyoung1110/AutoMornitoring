package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;

public interface RedisMediaService {
    void saveState(String traceId, String resolution, RecordMediaToRedisDTO dto);
    RecordMediaToRedisDTO getState(String traceId, String resolution);
    void pushHistory(String traceId, String resolution, RecordMediaToRedisDTO dto, int max);
}
