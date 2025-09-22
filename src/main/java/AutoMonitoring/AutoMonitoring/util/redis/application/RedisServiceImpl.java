package AutoMonitoring.AutoMonitoring.util.redis.application;

import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
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
    public boolean execute(String Script, String key, String ttl) {
        Long result = redisTemplate.execute(
                RedisScript.of(Script, Long.class),
                Collections.singletonList(key),
                ttl
        );
        if(result == 1L){
            log.info("큐에 작업을 삽입합니다. redisService Key: %s".formatted(key));
            return true;
        }
        else{
            log.info("큐에 작업이 존재합니다. redisService Key: %s".formatted(key));
            return false;
        }
    }

    @Override
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }
}
