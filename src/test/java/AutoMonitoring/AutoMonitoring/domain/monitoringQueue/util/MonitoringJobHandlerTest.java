package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Sender;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringJobHandlerTest {

    @Mock
    private GetMediaService getMediaService;
    @Mock
    private ParseMediaManifest parseMediaManifest;
    @Mock
    private RedisService redisService;
    @Mock
    private RabbitTemplate rabbitTemplate; // Still needed for sendDelay
    @Mock
    private Sender sender;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MonitoringJobHandler monitoringJobHandler;

    private CheckMediaManifestCmd testCmd;
    private CheckValidDTO testCheckValidDTO;

    @BeforeEach
    void setUp() {
        String traceId = UUID.randomUUID().toString();
        testCmd = new CheckMediaManifestCmd(
            "http://example.com/master.m3u8",
            "1080p",
            "test-agent",
            0,
            Instant.now(),
            traceId,
            1L
        );
        testCheckValidDTO = new CheckValidDTO(traceId, "1080p", Instant.now(), Duration.ZERO, 1L, 1L, Collections.emptyList(), 0, "", "", "", List.of(), false, false);
    }

    @Test
    @DisplayName("정상적인 경우, lock 획득, 미디어 파싱, 검증 메시지 전송, lock 해제를 수행해야 한다")
    void handle_successPath_shouldLockAndUnlock() throws JsonProcessingException {
        // given
        // 1. Epoch check passes
        when(redisService.getEpochReactive(anyString())).thenReturn(Mono.just(1L));
        // 2. Lock is acquired successfully
        when(redisService.getOpsAbsentReactive(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        // 3. Media fetch is successful
        when(getMediaService.getMediaNonBlocking(anyString(), anyString(), anyString())).thenReturn(Mono.just("hls_manifest_content"));
        // 4. Parsing is successful
        when(parseMediaManifest.parse(anyString(), any(Duration.class), anyString(), anyString())).thenReturn(testCheckValidDTO);
        // 5. DTO serialization is successful
        when(objectMapper.writeValueAsBytes(any(CheckValidDTO.class))).thenReturn(new byte[0]);
        // 6. Reactive send is successful
        when(sender.send(any(Mono.class))).thenReturn(Mono.empty());
        // 7. Lock release is successful
        when(redisService.deleteValuesReactive(anyString())).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(monitoringJobHandler.handle(testCmd))
                .verifyComplete();

        // verify
        verify(redisService).getEpochReactive(anyString());
        verify(redisService).getOpsAbsentReactive(anyString(), eq("1"), any(Duration.class));
        verify(getMediaService).getMediaNonBlocking(anyString(), anyString(), anyString());
        verify(parseMediaManifest).parse(anyString(), any(Duration.class), anyString(), anyString());
        verify(objectMapper).writeValueAsBytes(testCheckValidDTO);
        verify(sender).send(any(Mono.class));
        verify(redisService).deleteValuesReactive(anyString());
        // Verify that the old blocking rabbitTemplate is NOT called for the validation message
        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitNames.EX_VALID), anyString(), (Object) any());
    }
    
    @Test
    @DisplayName("Epoch이 일치하지 않으면, 메시지를 버려야 한다")
    void handle_staleEpoch_shouldDropMessage() {
        // given
        // Current epoch in Redis is 2, but command epoch is 1
        when(redisService.getEpochReactive(anyString())).thenReturn(Mono.just(2L));

        // when & then
        StepVerifier.create(monitoringJobHandler.handle(testCmd))
                .verifyComplete();
        
        // verify
        verify(redisService).getEpochReactive(anyString());
        verifyNoMoreInteractions(redisService, getMediaService, parseMediaManifest, rabbitTemplate, sender, objectMapper);
    }
}