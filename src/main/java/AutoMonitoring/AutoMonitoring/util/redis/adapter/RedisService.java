package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public interface RedisService {
    // Blocking methods
    void setValues(String key, String data);
    String getValues(String key);
    List<String> getValues(List<String> keys);
    Set<String> getKeys(String pattern);
    boolean getOpsAbsent(String key, String value, Duration ttl);
    boolean checkExistsValue(String value);
    boolean execute(String Script, String key, String ttl);
    long nextEpoch(String key);
    long getEpoch(String key);
    void deleteValues(String key);
    Long increment(String key, Long delta); // increment 메소드 추가
    void expire(String key, Duration ttl); // expire 메소드 추가

    // Reactive methods
    Mono<Boolean> getOpsAbsentReactive(String key, String value, Duration ttl);
    Mono<Void> deleteValuesReactive(String key);
    Mono<Long> nextEpochReactive(String key);
    Mono<Long> getEpochReactive(String key);
}
