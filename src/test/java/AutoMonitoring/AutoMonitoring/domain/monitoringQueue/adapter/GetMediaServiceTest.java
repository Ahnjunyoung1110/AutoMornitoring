package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.URLTestConfig;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception.SessionExpiredException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GetMediaServiceTest extends BaseTest {

    @Autowired
    private GetMediaService getMediaService;

    @MockitoBean
    private HttpClient httpClient; // Blocking HttpClient

    @Autowired
    private WebClient webClient; // Non-blocking WebClient

    static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        server.shutdown();
    }

    @BeforeEach
    void beforeEach(){
        // WebClient는 MockWebServer의 URL을 사용하도록 재구성
        // 이렇게 하면 각 테스트는 MockWebServer를 대상으로 실행됨
        // 이 부분은 실제 서비스에서 WebClient Bean이 어떻게 주입되는지에 따라 달라질 수 있음
        // 현재는 @Autowired WebClient가 httpbin.org로 고정되어 있으므로,
        // MockWebServer를 사용하려면 WebClient 설정을 변경해야 함 (테스트용 WebClient Bean 추가 또는 URL 재설정)
        // 여기서는 간단히 MockWebServer의 URL을 사용하도록 webClient를 직접 Mocking하지 않고
        // 테스트 메서드 내에서 MockWebServer의 URL을 사용하도록 함
        // 또는 WebClient의 base url을 BeforeEach에서 MockWebServer url로 설정하는 방법도 고려
        // 현재 WebClient는 spring context에 의해 생성된 것이므로, baseUrl을 변경하는 것이 복잡할 수 있음.
        // 각 테스트에서 MockWebServer URL을 직접 사용하여 요청을 보내는 것으로 처리합니다.
    }

    @Test
    @DisplayName("HTTP 요청이 성공(200 OK)하면, 응답 본문을 문자열로 반환한다.")
    void getMedia_WhenRequestSucceeds_ShouldReturnBodyAsString() throws IOException, InterruptedException {
        String expectedBody = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:10\n";

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> mockResponse = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        java.net.http.HttpHeaders plain = java.net.http.HttpHeaders.of(Map.of(), (k,v)->true);
        when(mockResponse.headers()).thenReturn(plain);
        when(mockResponse.body()).thenReturn(expectedBody.getBytes(StandardCharsets.UTF_8));

        when(httpClient.send(
                any(),
                ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<byte[]>>any()
        )).thenReturn(mockResponse);

        String actualBody = getMediaService.getMedia(URLTestConfig.SUCCESS_MANIFEST_URL, "TestAgent", "123");
        assertThat(actualBody).isEqualTo(expectedBody);
    }

    @Test
    @DisplayName("HTTP 요청이 실패(404 Not Found)하면, RuntimeException을 발생시킨다.")
    void getMedia_WhenRequestFails_ShouldThrowRuntimeException() throws IOException, InterruptedException {
        // given
        HttpResponse<byte[]> mockResponse = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(httpClient.send(any(), ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<byte[]>>any())).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of(), (k,v)->true));
        when(mockResponse.body()).thenReturn(new byte[0]);

        // when & then
        assertThatThrownBy(() -> getMediaService.getMedia(URLTestConfig.INVALID_URL, "TestAgent", "123"))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    @DisplayName("HttpClient가 예외를 발생시키면, RuntimeException으로 래핑하여 다시 발생시킨다.")
    void getMedia_WhenHttpClientThrowsException_ShouldWrapAndThrowRuntimeException() throws IOException, InterruptedException {
        // given
        when(httpClient.send(any(), any(java.net.http.HttpResponse.BodyHandler.class))).thenThrow(new IOException("Network error"));

        // when & then
        assertThatThrownBy(() -> getMediaService.getMedia(URLTestConfig.SUCCESS_MANIFEST_URL, "TestAgent", "123"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("HttpRequest를 비동기화로 실행한다. 해당 응답이 실제로 비동기로 실행되는지 확인한다.")
    void getMedia_with_non_blocking(){
        // MockWebServer에 느린 응답 Enqueue
        server.enqueue(new MockResponse().setBody("OK").setHeadersDelay(3, TimeUnit.SECONDS));

        // 1) 느린 엔드포인트 준비 (서버가 3초 뒤 응답)
        Mono<String> mono = getMediaService.getMediaNonBlocking(server.url("/delay/3").toString(), "hi", "123");


        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> onNextThread = new AtomicReference<>();
        AtomicLong onNextAt = new AtomicLong();
        AtomicLong subscribedAt = new AtomicLong();

        long beforeSubscribe = System.nanoTime();

        // 2) 구독(실제 실행 트리거). 콜백에서 스레드/시간 기록
        mono.doOnSubscribe(s -> subscribedAt.set(System.nanoTime()))
                .doOnNext(v -> {
                    onNextThread.set(Thread.currentThread().getName());
                    onNextAt.set(System.nanoTime());
                    done.countDown();
                })
                .subscribe(); // 비동기 실행 시작 (여기서 호출 스레드 반환됨)

        long afterSubscribe = System.nanoTime();

        // 3) subscribe 직후에도 호출 스레드가 자유로운지 확인 (동기 작업을 즉시 수행)
        long busyStart = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 50_000_000; i++) sum += i; // CPU 바쁜 작업 (수십 ms)
        long busyElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - busyStart);

        // 4) 5초 내 응답 도착 대기 (응답 콜백에서 latch countDown)
        boolean completed = false;
        try {
            completed = done.await(6, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // ====== 검증 포인트 ======

        // A. subscribe()는 즉시 리턴되어 다음 코드가 실행됨 (수 μs~ms 수준)
        long subscribeOverheadMs = TimeUnit.NANOSECONDS.toMillis(afterSubscribe - beforeSubscribe);
        assertTrue(subscribeOverheadMs < 50,
                "subscribe는 즉시 리턴해야 함. took=" + subscribeOverheadMs + "ms");

        // B. 응답은 3초 delay 뒤 도착해야 함 (콜백 시간 - subscribe 시간)
        long responseLatencyMs = TimeUnit.NANOSECONDS.toMillis(onNextAt.get() - subscribedAt.get());
        assertTrue(completed && responseLatencyMs >= 2900 && responseLatencyMs < 3100, // 3초 지연에 대한 허용 범위 추가
                "응답은 ~3초 뒤에 와야 함. latency=" + responseLatencyMs + "ms");

        // C. subscribe 직후 동기 작업이 문제없이 끝났는지 (스레드가 안 막혔음을 증명)
        assertTrue(busyElapsedMs > 0,
                "subscribe 이후에도 호출 스레드는 자유로워야 하며 동기 코드가 실행되어야 함");

        // D. 콜백 스레드가 reactor/netty 계열인지 확인 (이벤트루프에서 처리됨)
        assertNotNull(onNextThread.get(), "onNext 콜백 스레드 캡처 실패");
        assertTrue(onNextThread.get().contains("reactor") || onNextThread.get().contains("nio"),
                "콜백은 reactor/netty 스레드에서 실행되어야 함. actual=" + onNextThread.get());
    }

    @Test
    @DisplayName("getMediaNonBlocking: Publica 에러 헤더가 있을 경우 SessionExpiredException을 발생시킨다.")
    void getMediaNonBlocking_PublicaError_ThrowsSessionExpiredException() {
        // given
        String publicaErrorMessage = "failed retrieve state from cache: memcache: cache miss";
        server.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .addHeader("Publica-Error-Message", publicaErrorMessage)
                .setBody("Some error body"));

        // when
        Mono<String> mono = getMediaService.getMediaNonBlocking(server.url("/publica-error").toString(), "TestAgent", "trace-id-publica");

        // then
        StepVerifier.create(mono)
                .expectErrorMatches(throwable -> throwable instanceof SessionExpiredException &&
                        throwable.getMessage().contains("Publica session expired: " + publicaErrorMessage))
                .verify();
    }

    @Test
    @DisplayName("getMediaNonBlocking: Aniview 에러 JSON 바디가 있을 경우 SessionExpiredException을 발생시킨다.")
    void getMediaNonBlocking_AniviewError_ThrowsSessionExpiredException() {
        // given
        String aniviewErrorBody = "{\"error\":\"SES\",\"description\":\"session not found\"}";
        server.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aniviewErrorBody));

        // when
        Mono<String> mono = getMediaService.getMediaNonBlocking(server.url("/aniview-error").toString(), "TestAgent", "trace-id-aniview");

        // then
        StepVerifier.create(mono)
                .expectErrorMatches(throwable -> throwable instanceof SessionExpiredException &&
                        throwable.getMessage().contains("Aniview session expired: " + aniviewErrorBody))
                .verify();
    }

    @Test
    @DisplayName("getMediaNonBlocking: 일반적인 4xx 에러일 경우 UncheckedIOException을 발생시킨다.")
    void getMediaNonBlocking_Generic4xxError_ThrowsIOException() {
        // given
        server.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                );

        // when
        Mono<String> mono = getMediaService.getMediaNonBlocking(server.url("/generic-400-error").toString(), "TestAgent", "trace-id-generic");

        // then
        StepVerifier.create(mono)
                .expectErrorMatches(throwable -> throwable instanceof UncheckedIOException &&
                        throwable.getMessage().contains("HTTP 400 (Reactive) – URL Session issue? traceId=trace-id-generic"))
                .verify();
    }


    // Mockito.mock()을 정적 임포트하기 위한 헬퍼
    private static <T> T mock(Class<T> classToMock) {
        return org.mockito.Mockito.mock(classToMock);
    }
}