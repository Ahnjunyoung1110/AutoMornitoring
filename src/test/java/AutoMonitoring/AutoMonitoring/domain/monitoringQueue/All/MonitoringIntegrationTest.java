//package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.All;
//
//import AutoMonitoring.AutoMonitoring.BaseTest;
//import AutoMonitoring.AutoMonitoring.domain.api.dto.ProbeRequestDTO;
//import AutoMonitoring.AutoMonitoring.domain.api.service.UrlValidateCheck;
//import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
//import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
//import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.io.IOException;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.concurrent.TimeUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.argThat;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@AutoConfigureMockMvc
//public class MonitoringIntegrationTest extends BaseTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private RedisService redisService;
//
//    @MockitoBean
//    private HttpClient httpClient;
//
//
//    private static final String MASTER_URL = "http://test.com/master.m3u8";
//    private static final String MEDIA_URL_1080P = "http://test.com/1080p.m3u8";
//    private static final String MEDIA_URL_720P = "http://test.com/720p.m3u8";
//
//    @AfterEach
//    void tearDown() {
//        redisService.deleteValues(RedisKeys.state("trace-1", "1080p"));
//        redisService.deleteValues(RedisKeys.state("trace-1", "720p"));
//        redisService.deleteValues("trace-1");
//    }
//
//    @Test
//    @DisplayName("UC1: 마스터/미디어 Manifest가 모두 정상이면, 최종 상태가 MONITORING이 된다.")
//    void endToEnd_HappyPath() throws Exception {
//        // given: 모든 URL이 정상 응답을 반환하도록 설정
//        String masterManifest = createMasterManifest(MEDIA_URL_1080P, MEDIA_URL_720P);
//        String mediaManifest = createMediaManifest();
//        mockHttpResponse(MASTER_URL, 200, masterManifest);
//        mockHttpResponse(MEDIA_URL_1080P, 200, mediaManifest);
//        mockHttpResponse(MEDIA_URL_720P, 200, mediaManifest);
//
//        ProbeRequestDTO startDTO = new ProbeRequestDTO(MASTER_URL, "Test");
//
//        // when: 모니터링 시작 API 호출
//        mockMvc.perform(post("/probe")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(startDTO)))
//                .andExpect(status().isOk());
//
//        // then: 모든 해상도의 상태가 MONITORING으로 기록될 때까지 대기
//        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
//            assertThat(redisService.getValues(RedisKeys.state("trace-1", "1080p"))).isEqualTo("MONITORING");
//            assertThat(redisService.getValues(RedisKeys.state("trace-1", "720p"))).isEqualTo("MONITORING");
//            assertThat(redisService.getValues("trace-1")).isEqualTo("MONITORING");
//        });
//    }
//
//    @Test
//    @DisplayName("UC2: 마스터는 정상이지만 일부 미디어 Manifest가 비정상이면, 해당 해상도 상태가 FAILED가 된다.")
//    void endToEnd_PartialFailure() throws Exception {
//        // given: 1080p는 정상, 720p는 404 에러를 반환하도록 설정
//        String masterManifest = createMasterManifest(MEDIA_URL_1080P, MEDIA_URL_720P);
//        String mediaManifest = createMediaManifest();
//        mockHttpResponse(MASTER_URL, 200, masterManifest);
//        mockHttpResponse(MEDIA_URL_1080P, 200, mediaManifest);
//        mockHttpResponse(MEDIA_URL_720P, 404, "Not Found"); // 720p 비정상
//
//        ProbeRequestDTO startDTO = new ProbeRequestDTO(MASTER_URL, "Test");
//
//        // when: 모니터링 시작 API 호출
//        mockMvc.perform(post("/probe")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(startDTO)))
//                .andExpect(status().isOk());
//
//        // then: 1080p는 MONITORING, 720p는 FAILED 상태가 될 때까지 대기
//        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
//            assertThat(redisService.getValues(RedisKeys.state("trace-1", "1080p"))).isEqualTo("MONITORING");
//            assertThat(redisService.getValues(RedisKeys.state("trace-1", "720p"))).isEqualTo("FAILED");
//        });
//    }
//
//    @Test
//    @DisplayName("UC3: 마스터 Manifest부터 비정상이면, 전체 상태가 FAILED가 된다.")
//    void endToEnd_MasterFailure() throws Exception {
//        // given: 마스터 Manifest URL이 404 에러를 반환하도록 설정
//        mockHttpResponse(MASTER_URL, 404, "Not Found");
//
//        ProbeRequestDTO startDTO = new ProbeRequestDTO(MASTER_URL, "Test");
//
//        // when: 모니터링 시작 API 호출
//        mockMvc.perform(post("/probe")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(startDTO)))
//                .andExpect(status().is4xxClientError());
//
//        // then: 전체 상태가 FAILED로 기록될 때까지 대기
//        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
//            assertThat(redisService.getValues("trace-1")).isEqualTo("FAILED");
//        });
//    }
//
//    // --- Helper Methods ---
//
//    private void mockHttpResponse(String url, int statusCode, String body) throws IOException, InterruptedException {
//        HttpResponse<String> mockResponse = org.mockito.Mockito.mock(HttpResponse.class);
//        when(mockResponse.statusCode()).thenReturn(statusCode);
//        when(mockResponse.body()).thenReturn(body);
//        when(httpClient.send(
//                argThat((HttpRequest r) ->
//                        r != null && r.uri() != null && url.equals(r.uri().toString())),
//                any(HttpResponse.BodyHandler.class)
//        )).thenReturn(mockResponse);
//    }
//
//    private String createMasterManifest(String url1080, String url720) {
//        return String.format("""
//                #EXTM3U
//                #EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1920x1080
//                %s
//                #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1280x720
//                %s
//                """, url1080, url720);
//    }
//
//    private String createMediaManifest() {
//        return """
//                #EXTM3U
//                #EXT-X-VERSION:3
//                #EXT-X-TARGETDURATION:10
//                #EXT-X-MEDIA-SEQUENCE:1
//                #EXTINF:10.0,
//                segment1.ts
//                #EXTINF:10.0,
//                segment2.ts
//                """;
//    }
//}
