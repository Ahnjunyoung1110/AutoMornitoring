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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@DisplayName("ValidCheck 구현 예정")
class ValidateCheckServiceTest extends BaseTest {

    @Autowired
    private RedisMediaService redisMediaService;

    @Autowired
    private ValidateCheckService validateCheckService;

    @Autowired
    private ReactiveRedisTemplate<String, CheckValidDTO> reactiveRedisTemplate;

    private String traceId;
    private String resolution;

    @BeforeEach
    void setUp() {
        traceId = "validation-test-id";
        resolution = "1080p";
        reactiveRedisTemplate.delete(RedisKeys.hist(traceId, resolution)).block();
        reactiveRedisTemplate.delete(RedisKeys.hashState(traceId, resolution)).block();
    }

    @AfterEach
    void tearDown() {
        reactiveRedisTemplate.delete(RedisKeys.hist(traceId, resolution)).block();
        reactiveRedisTemplate.delete(RedisKeys.hashState(traceId, resolution)).block();
    }

    @Test
    @DisplayName("[OK_FINE] seq가 1씩 증가하고 segments 동일 → OK")
    void checkValidation_WhenSequenceIsConsecutive_ShouldReturnTrue() {
        // given
        CheckValidDTO record1 = createRecord(12345L);
        CheckValidDTO record2 = createRecord(12346L);
        redisMediaService.pushHistory(traceId, resolution, record1, 10).block();
        redisMediaService.pushHistory(traceId, resolution, record2, 10).block();

        // when & then
        StepVerifier.create(validateCheckService.checkValidation(createRecord(12347L)))
                .expectNext(ValidationResult.OK_FINE)
                .verifyComplete();
    }

    @Test
    @DisplayName("[ERROR_STALL_NO_PROGRESS] seq가 4회 동일 -> Error.")
    void checkValidation_WhenSequenceIsDuplicateSeveralTimes_ShouldReturnFalse() {
        // given
        CheckValidDTO record1 = createRecord(12345L); // 중복 시퀀스

        // when & then
        // 각 단계를 block()으로 동기적으로 실행하여 상태를 순차적으로 빌드
        StepVerifier.create(validateCheckService.checkValidation(record1)).expectNext(ValidationResult.OK_FINE).verifyComplete(); // 1
        StepVerifier.create(validateCheckService.checkValidation(record1)).expectNext(ValidationResult.OK_FINE).verifyComplete(); // 2
        StepVerifier.create(validateCheckService.checkValidation(record1)).expectNext(ValidationResult.OK_FINE).verifyComplete(); // 3
        StepVerifier.create(validateCheckService.checkValidation(record1)).expectNext(ValidationResult.ERROR_STALL_NO_PROGRESS).verifyComplete(); // 4
    }

    @Test
    @DisplayName("""
            [OK_FINE] seq 2 증가, segment는 정상 -> OK""")
    void checkValidation_WhenSequenceIsSkipped_ShouldReturnTrue() {
        // given
        CheckValidDTO record1 = new CheckValidDTO(
                traceId,
                resolution,
                Instant.now(),
                Duration.ofMillis(3),
                12345L,
                3L,
                new ArrayList<>(),
                10,
                "123",
                "ts121.ts",
                "ts130.ts",
                List.of("ts128.ts", "ts129.ts", "ts130.ts"),
                false
        );

        redisMediaService.pushHistory(traceId, resolution, record1, 10).block();

        CheckValidDTO record2 = new CheckValidDTO(
                traceId,
                resolution,
                Instant.now(),
                Duration.ofMillis(3),
                12347L, // 2 증가
                3L,
                new ArrayList<>(),
                10,
                "123",
                "ts123.ts", // 2 증가
                "ts132.ts", // 2 증가
                List.of("ts130.ts", "ts131.ts", "ts132.ts"),
                false
        );

        // when & then
        StepVerifier.create(validateCheckService.checkValidation(record2))
                .expectNext(ValidationResult.OK_FINE)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("[OK_FINE] 최초의 데이터 -> OK")
    void checkValidation_WhenFirstRecord_ShouldReturnTrue() {
        // when & then
        StepVerifier.create(validateCheckService.checkValidation(createRecord(12345L)))
                .expectNext(ValidationResult.OK_FINE)
                .verifyComplete();
    }

    @Test
    @DisplayName("[WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL] seq 증가, segment 유지 -> WARN")
    void checkValidation_WhenSequenceIsConsecutiveButSegmentNotChanged_ReturnWarn() {
        // given
        CheckValidDTO record1 = createRecord(12345L);
        redisMediaService.pushHistory(traceId, resolution, record1, 10).block();

        // when
         CheckValidDTO record2_same_segments = new CheckValidDTO(
                traceId,
                resolution,
                Instant.now(),
                Duration.ofMillis(2000),
                12346L, // New Seq
                123L,
                List.of(1,2),
                10,
                "",
                "http://test.url/segment12345.ts", // Old segment
                "http://test.url/segment12354.ts", // Old segment
                List.of("segment12352.ts", "segment12353.ts", "segment12354.ts"),
                false
        );
        
        // then
        StepVerifier.create(validateCheckService.checkValidation(record2_same_segments))
                .expectNext(ValidationResult.WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL)
                .verifyComplete();
    }

    @Test
    @DisplayName("[WARN_SEGMENTS_CHANGED_SEQ_STUCK] Seq 그대로, Seg 변경 -> [WARN]")
    void validateCheck_WhenSegmentIsChangedButSeqIsEqual_ReturnWarn() {
        // given
        CheckValidDTO record1 = createRecord(12345L);
        redisMediaService.pushHistory(traceId, resolution, record1, 10).block();

        // when
        CheckValidDTO record2 = new CheckValidDTO(
                traceId,
                resolution,
                Instant.now(),
                Duration.ofMillis(2000),
                12345L, // Same seq
                123L,
                List.of(1,2),
                10,
                "",
                "http://test.url/segment12346.ts", // New segment
                "http://test.url/segment12355.ts", // New segment
                List.of("103.ts", "104.ts"),
                false
        );

        // then
        StepVerifier.create(validateCheckService.checkValidation(record2))
                .expectNext(ValidationResult.WARN_SEGMENTS_CHANGED_SEQ_STUCK)
                .verifyComplete();
    }

    @Test
    @DisplayName("[ERROR_SEQ_REWIND] Seq 역행 -> ERROR")
    void validateCheck_SeqRewind_ReturnError(){
        // given
        CheckValidDTO record1 = createRecord(1234L);
        redisMediaService.pushHistory(traceId, resolution, record1, 10).block();

        // when
        CheckValidDTO record2 = createRecord(1233L);

        // then
        StepVerifier.create(validateCheckService.checkValidation(record2))
                .expectNext(ValidationResult.ERROR_SEQ_REWIND)
                .verifyComplete();
    }

    private CheckValidDTO createRecord(long seq) {
        return new CheckValidDTO(
                traceId,
                resolution,
                Instant.now(),
                Duration.ofMillis(2000),
                seq,
                123L,
                List.of(),
                10,
                "",
                "http://test.url/segment" + seq + ".ts",
                "http://test.url/segment" + (seq + 9) + ".ts",
                List.of("segment" + (seq+7) + ".ts", "segment" + (seq+8) + ".ts", "segment" + (seq+9) + ".ts"),
                false
        );
    }
}
