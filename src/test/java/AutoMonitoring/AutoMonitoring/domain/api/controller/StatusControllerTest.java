package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.domain.api.service.StatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatusController.class) // StatusController만 테스트
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 흉내내는 객체

    @MockitoBean
    private StatusService statusService; // 서비스 계층은 MockBean으로 대체

    @Test
    @DisplayName("GET /api/status/{traceId}: traceId의 메인 상태를 조회한다")
    void getTraceIdStatus() throws Exception {
        // given
        String traceId = "test-trace-id";
        String expectedStatus = "MONITORING";
        given(statusService.getTraceIdStatus(traceId)).willReturn(expectedStatus);

        // when & then
        mockMvc.perform(get("/api/status/{traceId}", traceId))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedStatus));
    }

    @Test
    @DisplayName("GET /api/status/{traceId}/details: traceId의 모든 상세 상태를 조회한다")
    void getAllStatuses() throws Exception {
        // given
        String traceId = "test-trace-id";
        Map<String, String> expectedStatusMap = Map.of(
                traceId, "MONITORING",
                "hls:test-trace-id:state:1080p", "RETRYING (2/5)",
                "hls:test-trace-id:state:720p", "MONITORING"
        );
        given(statusService.getAllStatusesForTraceId(traceId)).willReturn(expectedStatusMap);

        // when & then
        mockMvc.perform(get("/api/status/{traceId}/details", traceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(3))
                .andExpect(jsonPath("$.['hls:test-trace-id:state:1080p']").value("RETRYING (2/5)"));
    }

    @Test
    @DisplayName("GET /api/status/{traceId}: 존재하지 않는 traceId 조회 시 404를 반환한다")
    void getTraceIdStatus_NotFound() throws Exception {
        // given
        String traceId = "not-found-id";
        given(statusService.getTraceIdStatus(traceId)).willReturn("false");

        // when & then
        mockMvc.perform(get("/api/status/{traceId}", traceId))
                .andExpect(status().isNotFound());
    }
}
