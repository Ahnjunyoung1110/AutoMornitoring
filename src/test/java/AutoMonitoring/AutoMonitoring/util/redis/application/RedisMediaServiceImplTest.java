package AutoMonitoring.AutoMonitoring.util.redis.application;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisMediaServiceImplIT extends BaseTest {

    @Autowired RedisMediaService redisMediaService;

    String traceId;
    String resolution;
    CheckValidDTO dto;

    @BeforeEach
    void setUp() {
        traceId = "test-trace-id";
        resolution = "1920x1080";
        dto = new CheckValidDTO(
                "123",
                "1234",
                Instant.now(), Duration.ofMillis(2000),
                12345L, 67890L, List.of(1,2), 5,
                "hash-norm-value", "first-uri", "last-uri", List.of("123","456"), false
        );
    }

    @Test
    @DisplayName("saveState 후 getState로 동일 DTO가 복원된다")
    void save_and_get_roundtrip() {
        redisMediaService.saveState(traceId, resolution, dto);

        CheckValidDTO result = redisMediaService.getState(traceId, resolution);

        assertThat(result).isNotNull();
        assertThat(result.seq()).isEqualTo(dto.seq());
        assertThat(result.dseq()).isEqualTo(dto.dseq());
        assertThat(result.hashNorm()).isEqualTo(dto.hashNorm());
        assertThat(result.segFirstUri()).isEqualTo(dto.segFirstUri());
        assertThat(result.segLastUri()).isEqualTo(dto.segLastUri());
    }

    @Test
    @DisplayName("상태가 없으면 null")
    void get_not_found_returns_null() {
        CheckValidDTO result = redisMediaService.getState("nope", "640x360");
        assertThat(result).isNull();
    }
}
