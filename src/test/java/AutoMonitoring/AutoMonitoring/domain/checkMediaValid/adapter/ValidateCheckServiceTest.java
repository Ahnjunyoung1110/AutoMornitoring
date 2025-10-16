package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidCheck 구현 예정")
class ValidateCheckServiceTest extends BaseTest {

    @Autowired
    private RedisMediaService redisMediaService;

    @Autowired
    private ValidateCheckService validateCheckService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String traceId;
    private String resolution;

    @BeforeEach
    void setUp() {
        traceId = "validation-test-id";
        resolution = "1080p";
        // 각 테스트 전에 Redis 데이터 초기화
        redisTemplate.delete(RedisKeys.hist(traceId, resolution));
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후에 Redis 데이터 초기화
        redisTemplate.delete(RedisKeys.hist(traceId, resolution));
    }

    @Test
    @DisplayName("미디어 시퀀스가 순차적으로 증가하면 유효성 검사에 통과한다.")
    void checkValidation_WhenSequenceIsConsecutive_ShouldReturnTrue() {
        // given
        RecordMediaToRedisDTO record1 = createRecord(12345L);
        RecordMediaToRedisDTO record2 = createRecord(12346L);
        redisMediaService.pushHistory(traceId, resolution, record1, 10);
        redisMediaService.pushHistory(traceId, resolution, record2, 10);

        // when
        boolean isValid = validateCheckService.checkValidation(traceId, resolution);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("미디어 시퀀스가 중복되면 유효성 검사에 실패한다.")
    void checkValidation_WhenSequenceIsDuplicate_ShouldReturnFalse() {
        // given
        RecordMediaToRedisDTO record1 = createRecord(12345L);
        RecordMediaToRedisDTO record2 = createRecord(12345L); // 중복 시퀀스
        redisMediaService.pushHistory(traceId, resolution, record1, 10);
        redisMediaService.pushHistory(traceId, resolution, record2, 10);

        // when
        boolean isValid = validateCheckService.checkValidation(traceId, resolution);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("미디어 시퀀스가 순차적이지 않으면 (건너뛰면) 유효성 검사에 실패한다.")
    void checkValidation_WhenSequenceIsSkipped_ShouldReturnFalse() {
        // given
        RecordMediaToRedisDTO record1 = createRecord(12345L);
        RecordMediaToRedisDTO record2 = createRecord(12347L); // 시퀀스 건너뛰기
        redisMediaService.pushHistory(traceId, resolution, record1, 10);
        redisMediaService.pushHistory(traceId, resolution, record2, 10);

        // when
        boolean isValid = validateCheckService.checkValidation(traceId, resolution);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("검증할 데이터가 2개 미만이면 항상 통과한다.")
    void checkValidation_WhenLessThanTwoRecords_ShouldReturnTrue() {
        // given
        RecordMediaToRedisDTO record1 = createRecord(12345L);
        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        boolean isValid = validateCheckService.checkValidation(traceId, resolution);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("히스토리가 비어있을 때 유효성 검사에 통과한다.")
    void checkValidation_WhenHistoryIsEmpty_ShouldReturnTrue() {
        // given
        // 히스토리가 비어있는 상태

        // when
        boolean isValid = validateCheckService.checkValidation(traceId, resolution);

        // then
        assertThat(isValid).isTrue();
    }

    private RecordMediaToRedisDTO createRecord(long seq) {
        return new RecordMediaToRedisDTO(
                Instant.now(),
                Duration.ofMillis(2000),
                seq,
                123L,
                0,
                10,
                "",
                "http://test.url/segment" + seq + ".ts",
                "http://test.url/segment" + (seq + 9) + ".ts",
                "[]",
                false
        );
    }
}