package AutoMonitoring.AutoMonitoring.util.redis.adapter;

import java.util.List;

public interface RedisService {
    void setValues(String key, String data);
    String getValues(String key);
    boolean checkExistsValue(String value);

    //List<> getPreviousManifest
//    List<>

}
