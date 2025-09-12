package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;

public interface RedisMediaService {
    void saveState(String traceId, String resolution, RecordMediaToRedisDTO dto);
    RecordMediaToRedisDTO getState(String streamId, String resolution);
    void pushHistory(String streamId, String resolution, RecordMediaToRedisDTO dto, int max);
}
