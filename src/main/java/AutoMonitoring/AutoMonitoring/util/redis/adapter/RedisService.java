package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import java.time.Duration;

public interface RedisService {
    void setValues(String key, String data);
    String getValues(String key);
    boolean getOpsAbsent(String key, String value,Duration ttl);
    boolean checkExistsValue(String value);
    boolean execute(String Script, String key, String ttl);
    void deleteValues(String key);
}
