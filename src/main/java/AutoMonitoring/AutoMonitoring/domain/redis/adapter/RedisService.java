package AutoMonitoring.AutoMonitoring.domain.redis.adapter;

public interface RedisService {
    void setValues(String key, String data);
    String getValues(String key);
    boolean checkExistsValue(String value);
}
