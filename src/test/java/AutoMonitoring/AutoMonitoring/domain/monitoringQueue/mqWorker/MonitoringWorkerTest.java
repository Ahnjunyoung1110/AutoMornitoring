package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.URLTestConfig;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringWorkerTest extends BaseTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    private final String TRACE_ID = "test-trace-id-monitor";
    private final String RESOLUTION = "720p";

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        redisService.deleteValues(RedisKeys.state(TRACE_ID, RESOLUTION));
        while(rabbitTemplate.receive(RabbitNames.Q_WORK) != null);
        while(rabbitTemplate.receive(RabbitNames.Q_WORK_DLX) != null);
        while(rabbitTemplate.receive(RabbitNames.Q_DELAY_4S) != null);
    }

    @Test
    @DisplayName("정상 URL 수신 시, 다음 작업을 위해 지연 큐로 메시지를 보낸다")
    void receiveMessage_Success() {
        // given: 성공 URL을 담은 Command
        CheckMediaManifestCmd command = new CheckMediaManifestCmd(URLTestConfig.SUCCESS_MANIFEST_URL, RESOLUTION, "agent", 0, Instant.now(), TRACE_ID);
        redisService.setValues(RedisKeys.state(TRACE_ID, RESOLUTION), "MONITORING");

        // when: Worker가 메시지를 소비하도록 Q_WORK에 메시지 전송
        rabbitTemplate.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK, command);

        // then: 5초 지연에 해당하는 Q_DELAY_4S 큐로 메시지가 전송되어야 함
        Object received = rabbitTemplate.receiveAndConvert(RabbitNames.Q_DELAY_4S, 5500);
        assertThat(received).isNotNull();
        assertThat(((CheckMediaManifestCmd) received).traceId()).isEqualTo(TRACE_ID);

        // Redis 상태는 변경 없이 MONITORING을 유지해야 함
        String status = redisService.getValues(RedisKeys.state(TRACE_ID, RESOLUTION));
        assertThat(status).isEqualTo("MONITORING");
    }

    @Test
    @DisplayName("잘못된 URL 수신 시, 상태를 RETRYING (1/5)으로 변경하고 재시도 큐로 메시지를 보낸다")
    void receiveMessage_FirstFailure() {
        // given: 잘못된 URL을 담은 Command
        CheckMediaManifestCmd command = new CheckMediaManifestCmd(URLTestConfig.INVALID_URL, RESOLUTION, "agent", 0, Instant.now(), TRACE_ID);

        // when: Worker가 메시지를 소비하도록 Q_WORK에 메시지 전송
        registry.getListenerContainer("Retry_queue").stop();
        rabbitTemplate.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK, command);

        // then: 재시도 큐(Q_WORK_DLX)에서 메시지가 수신되어야 함
        Object received = rabbitTemplate.receiveAndConvert(RabbitNames.Q_WORK_DLX, 20000);
        assertThat(received).isNotNull();
        assertThat(((CheckMediaManifestCmd) received).traceId()).isEqualTo(TRACE_ID);

        // Redis 상태가 RETRYING (1/5)으로 변경되었는지 확인
        String status = redisService.getValues(RedisKeys.state(TRACE_ID, RESOLUTION));
        assertThat(status).isEqualTo("RETRYING (1/5)");
        registry.getListenerContainer("Retry_queue").start();
    }
}