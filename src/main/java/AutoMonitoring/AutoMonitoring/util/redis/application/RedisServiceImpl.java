package AutoMonitoring.AutoMonitoring.util.redis.application;

import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {


    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void setValues(String key, String data) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data);
    }


    @Override
    @Transactional(readOnly = true)
    public String getValues(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        if (values.get(key) == null) {
            return "false";
        }
        return (String) values.get(key);
    }

    @Override
    public boolean getOpsAbsent(String key, String value,Duration ttl) {
        Boolean Ok = redisTemplate.opsForValue().setIfAbsent(key,value, ttl);
        return Boolean.TRUE.equals(Ok);
    }

    @Override
    public boolean checkExistsValue(String value) {
        return !value.equals("false");
    }

    @Override
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }
}
