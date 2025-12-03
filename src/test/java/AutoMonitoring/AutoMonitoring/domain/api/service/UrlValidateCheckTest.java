package AutoMonitoring.AutoMonitoring.domain.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlValidateCheckTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<Void> mockHttpResponse;

    @InjectMocks
    private UrlValidateCheck urlValidateCheck;

    @Test
    @DisplayName("상대 경로가 포함된 URL이 올바르게 정규화되고 유효성 검사가 성공해야 한다")
    void check_shouldNormalizeRelativePathsAndReturnTrueForValidUrl() throws IOException, InterruptedException {
        // Given
        // 복잡한 상대 경로를 포함하는 URL. 정규화되면 간단한 경로가 되어야 함.
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        String problematicUrl = "http://example.com/a/b/../../c/./d/file.m3u8";
        // 예상되는 정규화된 URI
        URI expectedNormalizedUri = URI.create("http://example.com/c/d/file.m3u8");

        // When
        boolean isValid = urlValidateCheck.check(problematicUrl);

        // Then
        assertTrue(isValid, "URL 유효성 검사가 성공해야 한다.");
        // HttpClient.send가 예상되는 정규화된 URI로 호출되었는지 확인
        verify(mockHttpClient).send(argThat(request ->
                        request.uri().equals(expectedNormalizedUri) &&
                        request.method().equals("GET") &&
                        request.headers().firstValue("Range").orElse("").equals("bytes=0-0") &&
                        request.headers().firstValue("User-Agent").orElse("").equals("AutoMonitoring/1.0") &&
                        request.timeout().orElse(Duration.ZERO).equals(Duration.ofSeconds(10))
                ), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("잘못된 URL은 유효성 검사가 실패해야 한다")
    void check_shouldReturnFalseForInvalidUrl() throws IOException, InterruptedException {
        // Given
        String invalidUrl = "http://invalid-domain-that-does-not-exist.com/file.m3u8";
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("호스트를 찾을 수 없음")); // 네트워크 오류 모의

        // When
        boolean isValid = urlValidateCheck.check(invalidUrl);

        // Then
        assertFalse(isValid, "잘못된 URL은 유효성 검사가 실패해야 한다.");
    }

    @Test
    @DisplayName("응답 코드가 4xx 이상이면 유효성 검사가 실패해야 한다")
    void check_shouldReturnFalseForErrorStatusCode() {
        // Given
        String url = "http://example.com/nonexistent-file.m3u8";

        // When
        boolean isValid = urlValidateCheck.check(url);

        // Then
        assertFalse(isValid, "4xx 상태 코드에 대해 유효성 검사가 실패해야 한다.");
    }
}