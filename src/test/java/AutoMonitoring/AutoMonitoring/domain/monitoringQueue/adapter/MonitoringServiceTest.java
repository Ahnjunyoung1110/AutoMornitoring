package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.URLTestConfig;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.api.service.UrlValidateCheck;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MonitoringServiceTest extends BaseTest {

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisService redisService;

    @MockitoBean
    private UrlValidateCheck urlValidateCheck;

    @Autowired
    private RabbitListenerEndpointRegistry registry;



    @AfterEach
    void tearDown() {
        // 테스트 후 큐와 Redis 데이터 정리
        while(rabbitTemplate.receive(RabbitNames.Q_WORK) != null);
        redisService.deleteValues(RedisKeys.state("test-trace-id", "1080p"));
        redisService.deleteValues("test-trace-id");
    }

    @Test
    @DisplayName("유효한 URL로 모니터링 시작 시, Redis에 상태를 기록하고 지연 큐로 메시지를 보낸다.")
    void startMonitoring_WithValidUrl_ShouldRecordStateAndSendMessage() {
        // given
        StartMonitoringDTO dto = new StartMonitoringDTO("test-trace-id", URLTestConfig.SUCCESS_MANIFEST_URL, "1080p", "TestAgent");
        when(urlValidateCheck.check(anyString())).thenReturn(true);


        // when
        monitoringService.startMornitoring(dto);

        // then
        // 1. 메시지가 딜레이 큐로 발행되었는지 검증
        CheckMediaManifestCmd receivedCmd = (CheckMediaManifestCmd) rabbitTemplate.receiveAndConvert(RabbitNames.Q_DELAY_4S, 8000);
        assertThat(receivedCmd).isNotNull();
        assertThat(receivedCmd.traceId()).isEqualTo(dto.traceId());
        assertThat(receivedCmd.mediaUrl()).isEqualTo(dto.manifestUrl());
        assertThat(receivedCmd.failCount()).isZero();
    }

    @Test
    @DisplayName("유효하지 않은 URL로 모니터링 시작 시, Redis에 상태를 기록하고 예외를 발생시킨다.")
    void startMonitoring_WithInvalidUrl_ShouldRecordStateAndThrowException() {
        // given
        StartMonitoringDTO dto = new StartMonitoringDTO("test-trace-id", "invalid-url", "1080p", "TestAgent");
        when(urlValidateCheck.check(anyString())).thenReturn(false);

        // when & then
        // 1. 예외가 발생하는지 검증
        assertThatThrownBy(() -> monitoringService.startMornitoring(dto))
                .isInstanceOf(RuntimeException.class);

        // 2. Redis에 해상도별 상태가 'WRONG_URL'로 기록되었는지 검증
        String resolutionStateKey = RedisKeys.state(dto.traceId(), dto.resolution());
        assertThat(redisService.getValues(resolutionStateKey)).isEqualTo("WRONG_URL");

        // 3. 큐에 메시지가 없는지 검증
        assertThat(rabbitTemplate.receive(RabbitNames.Q_WORK)).isNull();
    }
}