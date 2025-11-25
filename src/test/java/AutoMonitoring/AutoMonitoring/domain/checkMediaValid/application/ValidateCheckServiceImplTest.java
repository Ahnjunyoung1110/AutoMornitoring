package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import AutoMonitoring.AutoMonitoring.contract.program.LogValidationFailureCommand;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidateCheckServiceImplTest extends BaseTest {

    @Autowired
    private ValidateCheckServiceImpl validateCheckService;

    @Autowired
    private RedisMediaService redisMediaService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("유효성 검사 실패 시, LogValidationFailureCommand를 발행한다")
    void checkValidation_SendsCommandOnFailure() throws IOException {
        // given
        String traceId = UUID.randomUUID().toString();
        String resolution = "1080p";
        long stuckSeq = 100L;

        // 'ERROR_STALL_NO_PROGRESS' 오류를 유발하기 위한 조건 설정
        // 동일한 seq를 가진 이전 기록을 2개 이상 쌓는다 (현재 DTO 포함 3개가 됨)
        CheckValidDTO prevDto = createTestCheckValidDTO(traceId, resolution, stuckSeq, "prev_last_uri");
        redisMediaService.pushHistory(traceId, resolution, prevDto, 10);
        redisMediaService.pushHistory(traceId, resolution, prevDto, 10);

        CheckValidDTO currDto = createTestCheckValidDTO(traceId, resolution, stuckSeq, "prev_last_uri");

        // when
        ValidationResult result = validateCheckService.checkValidation(currDto);

        // then
        // 1. 예상된 실패 결과가 반환되었는지 확인
        assertThat(result).isEqualTo(ValidationResult.ERROR_STALL_NO_PROGRESS);

        // 2. RabbitMQ 큐에 메시지가 발행되었는지 확인
        Message receivedMessage = rabbitTemplate.receive(RabbitNames.Q_STAGE2, 2000);
        assertThat(receivedMessage).isNotNull();

        // 3. 메시지 내용을 검증
        String messageBody = new String(receivedMessage.getBody(), StandardCharsets.UTF_8);
        LogValidationFailureCommand command = objectMapper.readValue(messageBody, LogValidationFailureCommand.class);

        assertThat(command.traceId()).isEqualTo(traceId);
    }

    private CheckValidDTO createTestCheckValidDTO(String traceId, String resolution, long seq, String lastUri) {
        return new CheckValidDTO(
                traceId,
                resolution,
                Instant.now(),
                Duration.ofMillis(150),
                seq,
                1,
                Collections.emptyList(),
                6,
                "some-hash-value",
                "first_uri.ts",
                lastUri + ".ts",
                List.of(lastUri + "_-2.ts", lastUri + "_-1.ts", lastUri + ".ts"),
                false
        );
    }
}
