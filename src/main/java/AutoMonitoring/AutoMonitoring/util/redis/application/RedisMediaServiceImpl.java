package AutoMonitoring.AutoMonitoring.util.redis.application;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisMediaServiceImpl implements RedisMediaService {

    private final ReactiveRedisTemplate<String, CheckValidDTO> redis;

    @Override
    public Mono<Void> saveState(String traceId, String resolution, CheckValidDTO dto) {
        String key = RedisKeys.hashState(traceId, resolution);
        return redis.opsForValue().set(key, dto, Duration.ofMinutes(3)).then();
    }

    @Override
    public Mono<CheckValidDTO> getState(String traceId, String resolution) {
        String key = RedisKeys.hashState(traceId, resolution);
        return redis.opsForValue().get(key);
    }

    @Override
    public Mono<Void> pushHistory(String traceId, String resolution, CheckValidDTO dto, int max) {
        String key = RedisKeys.hist(traceId, resolution);
        return redis.opsForList().leftPush(key, dto)
                .then(redis.opsForList().trim(key, 0, Math.max(0, max - 1)))
                .then();
    }

    @Override
    public Mono<List<CheckValidDTO>> getHistory(String traceId, String resolution) {
        String key = RedisKeys.hist(traceId, resolution);
        return redis.opsForList().range(key, 0, -1).collectList();
    }
}