package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {

    private final RedisService redisService;

    @Override
    public String getTraceIdStatus(String traceId) {
        return redisService.getValues(traceId);
    }

    @Override
    public Map<String, String> getAllStatusesForTraceId(String traceId) {
        Map<String, String> allStatuses = new TreeMap<>();

        // 1. 메인 traceId의 상태를 가져옵니다.
        String mainStatus = getTraceIdStatus(traceId);
        if (mainStatus != null && !mainStatus.equals("false")) {
            allStatuses.put(traceId, mainStatus);
        }

        // 2. SCAN을 사용하여 해상도별 상태 키를 모두 가져옵니다.
        String pattern = RedisKeys.state(traceId, "*");
        Set<String> stateKeys = redisService.getKeys(pattern);

        if (stateKeys == null || stateKeys.isEmpty()) {
            return allStatuses;
        }

        // 3. MGET을 사용하여 모든 상태 값을 한 번에 가져옵니다.
        List<String> keysList = new ArrayList<>(stateKeys);
        List<String> valuesList = redisService.getValues(keysList);

        // 4. 키와 값을 Map에 매핑합니다.
        for (int i = 0; i < keysList.size(); i++) {
            String key = keysList.get(i);
            String value = valuesList.get(i);
            if (value != null) {
                allStatuses.put(key, value);
            }
        }

        return allStatuses;
    }
}
