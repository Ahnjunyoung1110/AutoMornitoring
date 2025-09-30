package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.URLTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GetMediaServiceTest extends BaseTest {

    @Autowired
    private GetMediaService getMediaService;

    @MockitoBean
    private HttpClient httpClient;

    @Test
    @DisplayName("HTTP 요청이 성공(200 OK)하면, 응답 본문을 문자열로 반환한다.")
    void getMedia_WhenRequestSucceeds_ShouldReturnBodyAsString() throws IOException, InterruptedException {
        // given
        String expectedBody = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:10\n";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(expectedBody);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // when
        String actualBody = getMediaService.getMedia(URLTestConfig.SUCCESS_MANIFEST_URL, "TestAgent");

        // then
        assertThat(actualBody).isEqualTo(expectedBody);
    }

    @Test
    @DisplayName("HTTP 요청이 실패(404 Not Found)하면, RuntimeException을 발생시킨다.")
    void getMedia_WhenRequestFails_ShouldThrowRuntimeException() throws IOException, InterruptedException {
        // given
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // when & then
        assertThatThrownBy(() -> getMediaService.getMedia(URLTestConfig.INVALID_URL, "TestAgent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get media");
    }

    @Test
    @DisplayName("HttpClient가 예외를 발생시키면, RuntimeException으로 래핑하여 다시 발생시킨다.")
    void getMedia_WhenHttpClientThrowsException_ShouldWrapAndThrowRuntimeException() throws IOException, InterruptedException {
        // given
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("Network error"));

        // when & then
        assertThatThrownBy(() -> getMediaService.getMedia(URLTestConfig.SUCCESS_MANIFEST_URL, "TestAgent"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    // Mockito.mock()을 정적 임포트하기 위한 헬퍼
    private static <T> T mock(Class<T> classToMock) {
        return org.mockito.Mockito.mock(classToMock);
    }
}