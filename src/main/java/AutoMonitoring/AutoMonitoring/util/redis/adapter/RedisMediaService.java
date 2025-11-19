package AutoMonitoring.AutoMonitoring.util.redis.adapter;


import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;

import java.util.List;

public interface RedisMediaService {
    void saveState(String traceId, String resolution, CheckValidDTO dto);
    CheckValidDTO getState(String traceId, String resolution);
    void pushHistory(String traceId, String resolution, CheckValidDTO dto, int max);

    List<CheckValidDTO> getHistory(String traceId, String resolution);
}
