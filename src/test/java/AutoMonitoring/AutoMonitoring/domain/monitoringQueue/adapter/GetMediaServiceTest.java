package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.URLTestConfig;
import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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
    private HttpClient httpClient;

    @Autowired
    private WebClient webClient;

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
        // @BeforeEach 또는 테스트 시작 직후
        webClient.get().uri("https://httpbin.org/status/204")
                .retrieve().toBodilessEntity()
                .block(Duration.ofSeconds(2));  // 한 번 예열
    }

    @Test
    @DisplayName("HTTP 요청이 성공(200 OK)하면, 응답 본문을 문자열로 반환한다.")
    void getMedia_WhenRequestSucceeds_ShouldReturnBodyAsString() throws IOException, InterruptedException {
        String expectedBody = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:10\n";

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> mockResponse = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        HttpHeaders plain = HttpHeaders.of(Map.of(), (k,v)->true);
        when(mockResponse.headers()).thenReturn(plain);
        when(mockResponse.body()).thenReturn(expectedBody.getBytes(StandardCharsets.UTF_8));

        when(httpClient.send(
                any(),  // HttpRequest
                ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any() // ★ 제네릭 명시
        )).thenReturn(mockResponse);

        String actualBody = getMediaService.getMedia(URLTestConfig.SUCCESS_MANIFEST_URL, "TestAgent");
        assertThat(actualBody).isEqualTo(expectedBody);
    }

    @Test
    @DisplayName("HTTP 요청이 실패(404 Not Found)하면, RuntimeException을 발생시킨다.")
    void getMedia_WhenRequestFails_ShouldThrowRuntimeException() throws IOException, InterruptedException {
        // given
        HttpResponse<byte[]> mockResponse = (HttpResponse<byte[]>) mock(HttpResponse.class);

        when(httpClient.send(
                any(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()
        )).thenReturn(mockResponse);

        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (k,v)->true));
        when(mockResponse.body()).thenReturn(new byte[0]);

        // when & then
        assertThatThrownBy(() -> getMediaService.getMedia(URLTestConfig.INVALID_URL, "TestAgent"))
                .isInstanceOf(UncheckedIOException.class);
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

    @Test
    @DisplayName("HttpRequest를 비동기화로 실행한다. 해당 응답이 실제로 비동기로 실행되는지 확인한다.")
    void getMedia_with_non_blocking(){
        // 1) 느린 엔드포인트 준비 (서버가 3초 뒤 응답)
        Mono<String> mono = getMediaService.getMediaNonBlocking("https://httpbin.org/delay/3", "hi");


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
            completed = done.await(5, TimeUnit.SECONDS);
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
        assertTrue(completed && responseLatencyMs >= 2900,
                "응답은 ~3초 뒤에 와야 함. latency=" + responseLatencyMs + "ms");

        // C. subscribe 직후 동기 작업이 문제없이 끝났는지 (스레드가 안 막혔음을 증명)
        assertTrue(busyElapsedMs > 0,
                "subscribe 이후에도 호출 스레드는 자유로워야 하며 동기 코드가 실행되어야 함");

        // D. 콜백 스레드가 reactor/netty 계열인지 확인 (이벤트루프에서 처리됨)
        assertNotNull(onNextThread.get(), "onNext 콜백 스레드 캡처 실패");
        assertTrue(onNextThread.get().contains("reactor") || onNextThread.get().contains("nio"),
                "콜백은 reactor/netty 스레드에서 실행되어야 함. actual=" + onNextThread.get());
    }

    // Mockito.mock()을 정적 임포트하기 위한 헬퍼
    private static <T> T mock(Class<T> classToMock) {
        return org.mockito.Mockito.mock(classToMock);
    }
}