package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import java.time.Duration;
import java.util.List;

public interface RedisService {
    void setValues(String key, String data);
    String getValues(String key);
    boolean getOpsAbsent(String key, String value,Duration ttl);
    boolean checkExistsValue(String value);
    void deleteValues(String key);
}
