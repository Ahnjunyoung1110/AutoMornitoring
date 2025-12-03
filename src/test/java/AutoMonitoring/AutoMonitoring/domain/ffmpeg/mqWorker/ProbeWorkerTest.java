package AutoMonitoring.AutoMonitoring.domain.ffmpeg.mqWorker;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.program.DbProbeCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProbeDTO;
import AutoMonitoring.AutoMonitoring.contract.program.SaveM3u8State;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter.MediaProbe;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.ProbeCommand;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class ProbeWorkerTest extends BaseTest { // BaseTest 상속 유지

    @Autowired // 실제 객체 주입
    private ProbeWorker probeWorker;

    @Autowired // 실제 RabbitTemplate 주입 (Testcontainer)
    private RabbitTemplate rabbitTemplate;

    @Autowired // 실제 RedisService 주입 (Testcontainer)
    private RedisService redisService;

    @MockitoBean // MediaProbe는 실제 ffprobe를 실행하므로 MockBean으로 대체
    private MediaProbe mediaProbe;

    @AfterEach
    void tearDown() {
        // 테스트 간 독립성을 위해 Redis 데이터 정리
        redisService.deleteValues("test-trace-id");
        // 큐에 메시지가 남아있을 경우 다음 테스트에 영향을 주지 않도록 비워줌
        rabbitTemplate.receive(RabbitNames.Q_STAGE2);
    }

    @Test
    @DisplayName("Probe 성공 시, 다음 단계 메시지를 Q_STAGE2로 발행해야 한다")
    void handle_probe() {
        // given: 준비
        ProbeCommand command = new ProbeCommand("test-trace-id", "http://test.url", "test-agent");
        ProbeDTO fakeProbeResult = new ProbeDTO(command.traceId(), Instant.now(), command.masterUrl(), command.userAgent(), "hls", 0.0, 0, SaveM3u8State.WITHOUT_ADSLATE, Collections.emptyList(), Collections.emptyList());
        given(mediaProbe.probe(any(ProbeCommand.class))).willReturn(fakeProbeResult);

        // when: 실행
        probeWorker.handle(command);

        // then: 검증
        // Q_STAGE2에서 메시지를 실제로 수신하여 내용 검증
        Object received = rabbitTemplate.receiveAndConvert(RabbitNames.Q_STAGE2, 2000);
        assertThat(received).isInstanceOf(DbProbeCommand.class);
        assertThat(((DbProbeCommand) received).traceId()).isEqualTo("test-trace-id");
    }

    @Test
    @DisplayName("Probe 실패 시, Redis에 PROBE_FAILED 상태를 기록하고 메시지를 발행하지 않아야 한다")
    void handle_probefail_PROBE_FAILED_Record() {
        // given: 준비
        ProbeCommand command = new ProbeCommand("test-trace-id", "http://invalid.url", "test-agent");
        given(mediaProbe.probe(any(ProbeCommand.class))).willThrow(new RuntimeException("Probe failed"));

        // when: 실행
        probeWorker.handle(command);

        // then: 검증
        // Redis에 PROBE_FAILED 상태가 기록되었는지 확인
        String status = redisService.getValues("test-trace-id");
        assertThat(status).isEqualTo("PROBE_FAILED");

        // Q_STAGE2에 메시지가 발행되지 않았는지 확인
        Object received = rabbitTemplate.receiveAndConvert(RabbitNames.Q_STAGE2);
        assertThat(received).isNull();
    }
}