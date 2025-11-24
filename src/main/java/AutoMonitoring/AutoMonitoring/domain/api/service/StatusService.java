package AutoMonitoring.AutoMonitoring.domain.api.service;

import java.util.Map;

public interface StatusService {
    /**
     * traceId에 해당하는 모든 상태(메인 상태 + 해상도별 상태)를 조회합니다.
     * @param traceId 조회할 traceId
     * @return 상태 정보를 담은 Map (key: 상태 키, value: 상태 값)
     */
    Map<String, String> getAllStatusesForTraceId(String traceId);
}
