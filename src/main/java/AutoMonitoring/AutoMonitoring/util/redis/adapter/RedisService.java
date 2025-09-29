package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public interface RedisService {
    void setValues(String key, String data);
    String getValues(String key);
    List<String> getValues(List<String> keys); // MGET (Multi-Get)을 위한 메소드
    Set<String> getKeys(String pattern); // SCAN을 위한 메소드
    boolean getOpsAbsent(String key, String value,Duration ttl);
    boolean checkExistsValue(String value);
    boolean execute(String Script, String key, String ttl);
    void deleteValues(String key);
}
