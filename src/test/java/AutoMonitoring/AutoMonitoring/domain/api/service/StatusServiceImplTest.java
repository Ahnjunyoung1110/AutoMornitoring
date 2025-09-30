package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusServiceImplTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private StatusServiceImpl statusService;

    private String traceId;

    @BeforeEach
    void setUp() {
        traceId = "test-trace-id";
    }

    @Test
    @DisplayName("메인 traceId의 상태를 성공적으로 조회합니다.")
    void getTraceIdStatus_Success() {
        // given
        String expectedStatus = "PROCESSING";
        when(redisService.getValues(traceId)).thenReturn(expectedStatus);

        // when
        String actualStatus = statusService.getTraceIdStatus(traceId);

        // then
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("메인 traceId의 상태가 존재하지 않으면 null을 반환합니다.")
    void getTraceIdStatus_NotFound() {
        // given
        when(redisService.getValues(traceId)).thenReturn(null);

        // when
        String actualStatus = statusService.getTraceIdStatus(traceId);

        // then
        assertThat(actualStatus).isNull();
    }

    @Test
    @DisplayName("traceId에 해당하는 모든 상태(메인, 해상도별)를 성공적으로 조회합니다.")
    void getAllStatusesForTraceId_Success() {
        // given
        String mainStatus = "DONE";
        String pattern = RedisKeys.state(traceId, "*");
        String resolutionKey1 = "state:test-trace-id:1080p";
        String resolutionKey2 = "state:test-trace-id:720p";
        String resolutionStatus = "DONE";

        Set<String> stateKeys = new LinkedHashSet<>(Arrays.asList(resolutionKey1, resolutionKey2));
        List<String> stateValues = Arrays.asList(resolutionStatus, resolutionStatus);

        when(redisService.getValues(traceId)).thenReturn(mainStatus);
        when(redisService.getKeys(pattern)).thenReturn(stateKeys);
        when(redisService.getValues(anyList())).thenReturn(stateValues);

        // when
        Map<String, String> allStatuses = statusService.getAllStatusesForTraceId(traceId);

        // then
        assertThat(allStatuses).hasSize(3);
        assertThat(allStatuses).containsEntry(traceId, mainStatus);
        assertThat(allStatuses).containsEntry(resolutionKey1, resolutionStatus);
        assertThat(allStatuses).containsEntry(resolutionKey2, resolutionStatus);
    }

    @Test
    @DisplayName("메인 상태만 존재하고 해상도별 상태는 없는 경우 메인 상태만 조회합니다.")
    void getAllStatusesForTraceId_OnlyMainStatus() {
        // given
        String mainStatus = "PROCESSING";
        String pattern = RedisKeys.state(traceId, "*");

        when(redisService.getValues(traceId)).thenReturn(mainStatus);
        when(redisService.getKeys(pattern)).thenReturn(Collections.emptySet());

        // when
        Map<String, String> allStatuses = statusService.getAllStatusesForTraceId(traceId);

        // then
        assertThat(allStatuses).hasSize(1);
        assertThat(allStatuses).containsEntry(traceId, mainStatus);
    }

    @Test
    @DisplayName("조회된 상태가 하나도 없는 경우 빈 Map을 반환합니다.")
    void getAllStatusesForTraceId_NoStatusFound() {
        // given
        String pattern = RedisKeys.state(traceId, "*");
        when(redisService.getValues(anyString())).thenReturn(null);
        when(redisService.getKeys(pattern)).thenReturn(Collections.emptySet());

        // when
        Map<String, String> allStatuses = statusService.getAllStatusesForTraceId(traceId);

        // then
        assertThat(allStatuses).isEmpty();
    }
}
