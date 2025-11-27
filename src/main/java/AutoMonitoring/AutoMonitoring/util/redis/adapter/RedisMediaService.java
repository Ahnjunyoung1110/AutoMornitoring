package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RedisMediaService {
    Mono<Void> saveState(String traceId, String resolution, CheckValidDTO dto);
    Mono<CheckValidDTO> getState(String traceId, String resolution);
    Mono<Void> pushHistory(String traceId, String resolution, CheckValidDTO dto, int max);
    Mono<List<CheckValidDTO>> getHistory(String traceId, String resolution);
}