package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        redisTemplate.delete(RedisKeys.hist(traceId, resolution));
        redisTemplate.delete(RedisKeys.hashState(traceId, resolution));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(RedisKeys.hist(traceId, resolution));
        redisTemplate.delete(RedisKeys.hashState(traceId, resolution));
    }

    @Test
    @DisplayName("[OK_FINE] seq가 1씩 증가하고 segments 동일 → OK")
    void checkValidation_WhenSequenceIsConsecutive_ShouldReturnTrue() {
        // given
        CheckValidDTO record1 = createRecord(12345L);
        CheckValidDTO record2 = createRecord(12346L);
        redisMediaService.pushHistory(traceId, resolution, record1, 10);
        redisMediaService.pushHistory(traceId, resolution, record2, 10);

        // when
        ValidationResult isValid = validateCheckService.checkValidation(createRecord(12347L));

        // then
        assertThat(isValid).isEqualTo(ValidationResult.OK_FINE);
    }

    @Test
    @DisplayName("[WARN_NO_CHANGE] seq가 1회 동일 -> Warn.")
    void checkValidation_WhenSequenceIsDuplicate_ShouldReturnFalse() {
        // given
        CheckValidDTO record1 = createRecord(12345L); // 중복 시퀀스
        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        ValidationResult isValid = validateCheckService.checkValidation(createRecord(12345L)); // 중복 시퀀스

        // then
        assertThat(isValid).isEqualTo(ValidationResult.WARN_NO_CHANGE);
    }

    @Test
    @DisplayName("[ERROR_STALL_NO_PROGRESS] seq가 3회 동일 -> Error.")
    void checkValidation_WhenSequenceIsDuplicateSeveralTimes_ShouldReturnFalse() {
        // given
        CheckValidDTO record1 = createRecord(12345L); // 중복 시퀀스 3회

        // when
        ValidationResult isValid;
        isValid = validateCheckService.checkValidation(createRecord(12345L)); // 중복 시퀀스 1회째 [OK_FINE]
        assertThat(isValid).isEqualTo(ValidationResult.OK_FINE);

        isValid = validateCheckService.checkValidation(createRecord(12345L)); // 중복 시퀀스 2회째 [OK_FINE]
        assertThat(isValid).isEqualTo(ValidationResult.OK_FINE);

        isValid = validateCheckService.checkValidation(createRecord(12345L)); // 중복 시퀀스 3회째 [OK_FINE]
        assertThat(isValid).isEqualTo(ValidationResult.OK_FINE);

        isValid = validateCheckService.checkValidation(createRecord(12345L)); // 중복 시퀀스 4회째 [ERROR_STALL_NO_PROGRESS]

        // then
        assertThat(isValid).isEqualTo(ValidationResult.ERROR_STALL_NO_PROGRESS);
    }

    @Test
    @DisplayName("""
            [OK_FINE] seq 2 증가, segment는 정상 -> OK""")
    void checkValidation_WhenSequenceIsSkipped_ShouldReturnTrue() {
        // given
        CheckValidDTO record1 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://121.ts",
                "https://121310.ts",
                List.of("121.ts", "122.ts", "123.ts"),
                false
        );

        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        CheckValidDTO record2 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12347L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://123.ts",
                "https://121312.ts",
                List.of("123.ts", "124.ts", "125.ts"),
                false
                );
        ValidationResult isValid = validateCheckService.checkValidation(record2); // 시퀀스 건너뛰기

        // then
        assertThat(isValid).isEqualTo(ValidationResult.OK_FINE);
    }

    @Test
    @DisplayName("[OK_FINE] seq 3 증가,segment는 정상 -> OK ")

    void checkValidation_WhenSequenceIsSkipped3_ShouldReturnTrue() {
        // given
        CheckValidDTO record1 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://121.ts",
                "https://121310.ts",
                List.of("121.ts", "122.ts", "123.ts"),
                false
        );

        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        CheckValidDTO record2 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12348L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://124.ts",
                "https://121313.ts",
                List.of("124.ts", "125.ts", "126.ts"),
                false
        );
        ValidationResult isValid = validateCheckService.checkValidation(record2); // 시퀀스 건너뛰기

        // then
        assertThat(isValid).isEqualTo(ValidationResult.OK_FINE);
    }



    @Test
    @DisplayName("[OK_FINE] 최초의 데이터 -> OK")
    void checkValidation_WhenFirstRecord_ShouldReturnTrue() {
        // given
        // when
        ValidationResult isValid = validateCheckService.checkValidation(createRecord(12345L));

        // then
        assertThat(isValid).isEqualTo(ValidationResult.OK_FINE);
    }

    @Test
    @DisplayName("[WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL] seq 증가, segment 유지 -> WARN")
    void checkValidation_WhenSequenceIsConsecutiveButSegmentNotChanged_ReturnWarn() {
        // given
        CheckValidDTO record1 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://121.ts",
                "https://121310.ts",
                List.of("121.ts", "122.ts", "123.ts"),
                false
        );

        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        CheckValidDTO record2 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12346L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://121.ts",
                "https://121310.ts",
                List.of("121.ts", "122.ts", "123.ts"),
                false
        );
        ValidationResult isValid = validateCheckService.checkValidation(record2);

        // then
        assertThat(isValid).isEqualTo(ValidationResult.WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL);
    }

    @Test
    @DisplayName("[WARN_SEGMENTS_CHANGED_SEQ_STUCK] Seq 그대로, Seg 변경 -> [WARN]")
    void validateCheck_WhenSegmentIsChangedButSeqIsEqual_ReturnWarn() {
        // given
        CheckValidDTO record1 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://121.ts",
                "https://121310.ts",
                List.of("121.ts", "122.ts", "123.ts"),
                false
        );

        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        CheckValidDTO record2 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "https://122.ts",
                "https://121311.ts",
                List.of("122.ts", "123.ts", "124.ts"),
                false
        );
        ValidationResult isValid = validateCheckService.checkValidation(record2);

        // then
        assertThat(isValid).isEqualTo(ValidationResult.WARN_SEGMENTS_CHANGED_SEQ_STUCK);

    }

    @Test
    @DisplayName("[WARN_DSEQ_STALE_AFTER_REMOVAL] DisCount변경, Dseq 미변경 -> WARN")
    void validateCheck_WhenDiscontinuityDisapearButDseqNotChanged_ReturnWarn(){
        // given
        CheckValidDTO record1 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                List.of(1,2),
                10,
                "123",
                "https://121.ts",
                "https://121310.ts",
                List.of("121.ts", "122.ts", "123.ts"),
                false
        );

        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        CheckValidDTO record2 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                List.of(1,2),
                10,
                "123",
                "https://122.ts",
                "https://121311.ts",
                List.of("122.ts", "123.ts", "124.ts"),
                false
        );
        ValidationResult isValid = validateCheckService.checkValidation(record2);

        // then
        assertThat(isValid).isEqualTo(ValidationResult.WARN_DSEQ_STALE_AFTER_REMOVAL);

    }

    @Test
    @DisplayName("[ERROR_SEQ_REWIND] Seq 역행 -> ERROR")
    void validateCheck_SeqRewind_ReturnError(){
        // given
        CheckValidDTO record1 = createRecord(1234L);

        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        CheckValidDTO record2 = createRecord(1233L);
        ValidationResult isValid = validateCheckService.checkValidation(record2);

        // then
        assertThat(isValid).isEqualTo(ValidationResult.ERROR_SEQ_REWIND);
    }

    @Test
    @DisplayName("[ERROR_SEGMENT_GAP_OR_OVERLAP] 같은 Segment -> ERROR")
    void validateCheck_SameSegmentReappearance_ReturnError(){
        // given
        CheckValidDTO record1 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                List.of(1,2),
                10,
                "123",
                "https://121.ts",
                "https://121310.ts",
                List.of("121.ts", "122.ts", "123.ts"),
                false
        );

        redisMediaService.pushHistory(traceId, resolution, record1, 10);

        // when
        CheckValidDTO record2 = new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(3),
                12346L,
                3L,
                List.of(1,2),
                10,
                "124",
                "https://123.ts",
                "https://121312.ts",
                List.of("122.ts", "122.ts", "124.ts"),
                false
        );
        ValidationResult isValid = validateCheckService.checkValidation(record2);

        // then
        assertThat(isValid).isEqualTo(ValidationResult.ERROR_SEGMENT_GAP_OR_OVERLAP);
    }






    private CheckValidDTO createRecord(long seq) {
        return new CheckValidDTO(
                Instant.now(),
                Duration.ofMillis(2000),
                seq,
                123L,
                List.of(1,2),
                10,
                "",
                "http://test.url/segment" + seq + ".ts",
                "http://test.url/segment" + (seq + 9) + ".ts",
                List.of("102.ts", "103.ts"),
                false
        );
    }
}