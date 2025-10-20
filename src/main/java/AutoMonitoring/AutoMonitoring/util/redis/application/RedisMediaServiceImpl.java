package AutoMonitoring.AutoMonitoring.util.redis.application;

import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.dto.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisMediaServiceImpl implements RedisMediaService {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    @Override
    public void saveState(String traceId, String resolution, CheckValidDTO dto) {
        String m = null;
        try {
            m = om.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String key = RedisKeys.hashState(traceId,resolution);
        redis.opsForValue().set(key, m, Duration.ofMinutes(3));
    }

    @Override
    public CheckValidDTO getState(String traceId, String resolution) {
        String key = RedisKeys.hashState(traceId,resolution);
        String getDTO = redis.opsForValue().get(key);
        if(getDTO == null) return null;
        try {
            return om.readValue(getDTO, CheckValidDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pushHistory(String traceId, String resolution, CheckValidDTO dto, int max) {
        try {
            max = 10;
            String json = om.writeValueAsString(dto);
            String key = RedisKeys.hist(traceId, resolution);
            redis.opsForList().leftPush(key, json);
            redis.opsForList().trim(key, 0, Math.max(0, max - 1));
        } catch (Exception e) {
            throw new RuntimeException("pushHistory failed", e);
        }
    }

    @Override
    public List<CheckValidDTO> getHistory(String traceId, String resolution) {
        try {
            String key = RedisKeys.hist(traceId, resolution);
            List<String> jsons = redis.opsForList().range(key, 0, -1); // 0이 최신(LEFT push 했으므로)
            if (jsons == null || jsons.isEmpty()) return List.of();

            List<CheckValidDTO> out = new ArrayList<>(jsons.size());
            for (String j : jsons) {
                out.add(om.readValue(j, CheckValidDTO.class));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("getHistory failed", e);
        }
    }
}
