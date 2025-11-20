package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class DelayMonitoringWorkerTest extends BaseTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    @Autowired
    private RedisService redisService;

    @MockitoBean // 실제 HTTP 요청을 보내지 않도록 HttpClient를 MockBean으로 대체
    private HttpClient httpClient;

    private final String TRACE_ID = "test-trace-id";
    private final String RESOLUTION = "1080p";

    @BeforeEach
    void tearDown() {
        redisService.deleteValues(RedisKeys.state(TRACE_ID, RESOLUTION));
        // 테스트에 사용된 큐들을 비워줍니다.
        while(rabbitTemplate.receive(RabbitNames.Q_WORK) != null);
        while(rabbitTemplate.receive(RabbitNames.Q_RETRY_DELAY_1S) != null);
        while(rabbitTemplate.receive(RabbitNames.Q_DEAD) != null);
        while(rabbitTemplate.receive(RabbitNames.Q_WORK_DLX) != null);
    }

    @Test
    @DisplayName("재시도 성공 시, 상태를 MONITORING으로 변경하고 주 모니터링 큐로 메시지를 보낸다")
    void receiveMessage_RetrySuccess() throws IOException, InterruptedException {
        // given: 재시도할 메시지를 재시도 큐(Q_WORK_DLX)에 직접 전송
        CheckMediaManifestCmd command = new CheckMediaManifestCmd("http://test.url", RESOLUTION, "agent", 0, Instant.now(), TRACE_ID, 0L);
        given(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).willReturn(new MockHttpResponse(200));

        // when: Worker가 메시지를 소비하도록 메시지 전송
        // queue에 입력되는것을 확인해야 하므로 소비자 off
        registry.getListenerContainer("Retry_queue").start();
        rabbitTemplate.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK_DLX, command);

        // then: 주 모니터링 큐(Q_WORK)에서 메시지가 수신되어야 함
        Object received = rabbitTemplate.receiveAndConvert(RabbitNames.Q_WORK, 2000);
        assertThat(received).isNotNull();
        assertThat(((CheckMediaManifestCmd) received).traceId()).isEqualTo(TRACE_ID);

        // Redis 상태가 MONITORING으로 변경되었는지 확인
        String status = redisService.getValues(RedisKeys.state(TRACE_ID, RESOLUTION));
        assertThat(status).isEqualTo("MONITORING");

        // 완료 후 다시 가동
        registry.getListenerContainer("Retry_queue").stop();
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시, 상태를 FAILED로 변경하고 메시지를 Dead Letter 큐로 보낸다")
    void receiveMessage_MaxRetriesExceeded() {
        // given: 재시도 횟수가 4번인 메시지 준비 (이번이 5번째 시도)
        CheckMediaManifestCmd command = new CheckMediaManifestCmd("http://test.url", RESOLUTION, "agent", 0, Instant.now(), TRACE_ID, 0L);
        MessageProperties properties = new MessageProperties();
        List<Map<String, Object>> xDeath = new ArrayList<>();
        Map<String, Object> deathHeader = new HashMap<>();
        deathHeader.put("count", 4L); // 재시도 횟수 4번
        deathHeader.put("queue", RabbitNames.Q_RETRY_DELAY_1S);
        xDeath.add(deathHeader);
        properties.setHeader("x-death", xDeath);

        Message message = rabbitTemplate.getMessageConverter().toMessage(command, properties);

        // when: Worker가 메시지를 소비하도록 메시지 전송
        registry.getListenerContainer("Retry_queue").start();
        rabbitTemplate.send(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK_DLX, message);

        // then: Dead Letter 큐(Q_DEAD)에서 메시지가 수신되어야 함
        Object received = rabbitTemplate.receiveAndConvert(RabbitNames.Q_DEAD, 15000);
        assertThat(received).isNotNull();

        // Redis 상태가 FAILED로 변경되었는지 확인
        String status = redisService.getValues(RedisKeys.state(TRACE_ID, RESOLUTION));
        assertThat(status).isEqualTo("FAILED");
        registry.getListenerContainer("Retry_queue").stop();
    }

    // HttpClient Mock을 위한 간단한 구현체
    private static class MockHttpResponse implements HttpResponse<Void> {
        private final int statusCode;
        public MockHttpResponse(int statusCode) { this.statusCode = statusCode; }
        @Override public int statusCode() { return statusCode; }
        @Override public HttpRequest request() { return null; }
        @Override public Optional<HttpResponse<Void>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return null; }
        @Override public Void body() { return null; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return null; }
        @Override public HttpClient.Version version() { return null; }
    }
}