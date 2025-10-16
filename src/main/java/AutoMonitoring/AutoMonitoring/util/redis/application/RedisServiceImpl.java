package AutoMonitoring.AutoMonitoring.util.redis.application;

import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public List<String> getValues(List<String> keys) {
        // MGET을 실행하여 여러 키의 값을 한번에 가져옵니다.
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        // RedisTemplate<String, Object>를 사용하므로, 결과를 String 리스트로 변환합니다.
        return values.stream()
                .map(value -> value != null ? String.valueOf(value) : null)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getKeys(String pattern) {
        Set<String> keys = new HashSet<>();

        // SCAN 명령을 실행하여 패턴에 맞는 키를 안전하게 검색합니다.
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            // try-with-resources를 사용하여 Cursor를 안전하게 닫습니다.
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            }
            return null;
        });
        return keys;
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
