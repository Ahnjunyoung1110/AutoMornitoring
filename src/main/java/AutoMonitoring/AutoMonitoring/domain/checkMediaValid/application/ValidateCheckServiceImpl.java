package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import AutoMonitoring.AutoMonitoring.contract.program.LogValidationFailureCommand;
import AutoMonitoring.AutoMonitoring.contract.program.SaveFailureDTO;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter.ValidateCheckService;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidateCheckServiceImpl implements ValidateCheckService {

    private final RedisMediaService redis;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Mono<ValidationResult> checkValidation(CheckValidDTO dto) {
        // redis.getHistory is now reactive
        return redis.getHistory(dto.traceId(), dto.resolution())
                .flatMap(dtos -> {
                    if (dtos.isEmpty()) {
                        return Mono.just(ValidationResult.OK_FINE);
                    }

                    // 1) Run checks
                    ValidationResult result = checkChunkList(dtos, dto);
                    if (result == ValidationResult.OK_FINE) {
                        result = invalidSequenceChange(dtos, dto);
                    }

                    // 2) If a validation failed, send a message
                    if (result != ValidationResult.OK_FINE) {
                        SaveFailureDTO failure = buildFailureDTO(result, dtos, dto);
                        LogValidationFailureCommand command = new LogValidationFailureCommand(dto.traceId(), failure);

                        // Wrap the blocking send call
                        return Mono.fromRunnable(() ->
                                        rabbitTemplate.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE2, command)
                                )
                                .subscribeOn(Schedulers.boundedElastic())
                                .thenReturn(result); // Return the result after sending
                    }

                    return Mono.just(result);
                })
                .defaultIfEmpty(ValidationResult.OK_FINE) // If history was empty, default to OK
                .doFinally(signalType -> {
                    // Always update history, regardless of outcome
                    redis.pushHistory(dto.traceId(), dto.resolution(), dto, 10)
                            .subscribe(); // Fire-and-forget
                });
    }

    // 1. 미디어 시퀀스는 변했으나 chunk의 목록이 바뀌지 않은 경우
    public ValidationResult checkChunkList(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {
        // 바로 이전의 m3u8
        CheckValidDTO prev = previousDTOs.getFirst();

        // seq 증가가 아니면 이 룰의 대상이 아님 (동일/역행은 다른 룰에서 처리)
        if (curr.seq() <= prev.seq()) return ValidationResult.OK_FINE;

        // "m3u8 내용물이" 동일한지 보수적으로 확인
        boolean segmentsIdentical =
                prev.segmentCount() == curr.segmentCount() &&
                        safeEq(prev.segFirstUri(), curr.segFirstUri()) &&
                        safeEq(prev.segLastUri(), curr.segLastUri()) &&
                        listEq(prev.tailUris(), curr.tailUris());

        if (segmentsIdentical) {
            return ValidationResult.WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL;
        }
        return ValidationResult.OK_FINE;
    }

    // 2. 미디어 시퀀스가 1이 아니게 변한 경우
    public ValidationResult invalidSequenceChange(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {
        // 바로 이전의 m3u8
        CheckValidDTO prev = findPrevBySeq(previousDTOs, curr);
        long seqDifference = curr.seq() - prev.seq();

        // 정상의 경우
        if (seqDifference == 1) return ValidationResult.OK_FINE;

        // 2.1 같은 경우
        // -> 3회 이상 확인하여 에러 생성, seg가 변경되었다면 warn
        if (seqDifference == 0) {
            // seg 변경 확인
            if (!Objects.equals(curr.segFirstUri(), prev.segFirstUri())) {
                return ValidationResult.WARN_SEGMENTS_CHANGED_SEQ_STUCK;
            }

            int sameCount = 1; // 현재 포함
            long currSeq = curr.seq();
            // 이전것들이 같은지 확인
            for (CheckValidDTO dto : previousDTOs) {
                if (dto.seq() == currSeq) {
                    sameCount++;
                } else {
                    // 연속 구간만 보면 되므로 다른 값 나오면 중단
                    break;
                }
            }

            if (sameCount >= 4) {
                return ValidationResult.ERROR_STALL_NO_PROGRESS;
            }
        }

        // 2.2 7 이상 늘어난 경우
        if (seqDifference >= 7) {
            return ValidationResult.WARN_SEQUENCE_CHANGE_TOO_FAR;
        }

        // 2.3 2 이상 늘어난 경우
        // -> 인터넷에 의해 파일이 밀렸을 수 있으므로 청크가 새로 입력된 경우 문제가 아니라고 판단
        if (seqDifference >= 2) {
            Long prevFirst = num(prev.segFirstUri());
            Long currFirst = num(curr.segFirstUri());
            Long prevLast = num(prev.segLastUri());
            Long currLast = num(curr.segLastUri());
            boolean canCheckNumber =
                    prevFirst != null && currFirst != null
                            && prevLast != null && currLast != null;

            // 이전 데이터가 존재해야 확인이 가능하다. 또한 discontinuity 가 존재하는 경우 이를 검사하지 않는다.
            if (canCheckNumber) {
                long firstDiff = currFirst - prevFirst;
                long lastDiff = currLast - prevLast;

                // seq 증가량과 segment 번호 증가량이 정확히 맞으면
                // "파일이 밀렸지만 연속성은 유지"로 판단
                if (firstDiff == seqDifference && lastDiff == seqDifference) {
                    return ValidationResult.OK_FINE;
                } else {
                    // discontinuity 가 존재한다면 연속성이 유지되지 않을 수 있음
                    if (!curr.discontinuityPos().isEmpty() || !prev.discontinuityPos().isEmpty()) {
                        return ValidationResult.OK_FINE;
                    }
                    return ValidationResult.WARN_MEDIA_SEQUENCE_SEGMENT_MISMATCH;
                }
            }
        }

        // 2.4 줄어든 경우 – 심각한 에러
        if (seqDifference < 0) {
            return ValidationResult.ERROR_SEQ_REWIND;
        }

        return ValidationResult.OK_FINE; // 여기까지 올 일은 사실상 없음
    }

    // 3. discontinuity 시퀀스 관련 (현재 미구현)
    public ValidationResult checkDiscontinuity(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {
        return ValidationResult.OK_FINE;
    }

    // 4. 이전 .m3u8과 청크의 갯수가 크게 차이나는 경우 (미구현)
    public ValidationResult checkChunkCount() {
        return ValidationResult.OK_FINE;
    }

    private static boolean safeEq(String a, String b) {
        return Objects.equals(a, b);
    }

    private static boolean listEq(List<String> a, List<String> b) {
        if (a == null || a.isEmpty()) return b == null || b.isEmpty();
        if (b == null || b.isEmpty()) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    // 가장 최근 seq를 확인한다.
    private CheckValidDTO findPrevBySeq(List<CheckValidDTO> history, CheckValidDTO curr) {
        return history.stream()
                .filter(h -> h.seq() < curr.seq()) // 나보다 과거인 것들만
                .max((a, b) -> Long.compare(a.seq(), b.seq())) // seq 가장 큰 놈
                .orElse(history.getFirst()); // 못 찾으면 기존 fallback
    }

    /**
     * URL 문자열 어디에 있든 ts숫자.ts 패턴을 찾아 숫자 부분을 시퀀스로 해석
     */
    private static Long num(String s) {
        // URL 안에 u= 로 인코딩된 ts0408.ts 같은 것도 있을 수 있으니까,
        // 일단 한 번 디코딩 시도 (깨지면 그냥 원문으로 진행)
        try {
            s = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            // 디코딩 실패해도 그냥 s 그대로 사용
        }

        // 어디에 있든 "ts숫자.ts" 패턴만 찾는다
        Pattern P = Pattern.compile("ts(\\d+)\\.ts");
        Matcher m = P.matcher(s);

        if (!m.find()) {
            return null;   // ts숫자.ts가 없는 경우 → 시퀀스 없음 처리
        }

        try {
            return Long.parseLong(m.group(1)); // 0408 이런 거
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * ValidationResult / 이전 히스토리 / 현재 DTO를 기반으로
     * SaveFailureDTO + detailReason/prev/expected 값까지 채움
     */
    private SaveFailureDTO buildFailureDTO(ValidationResult result,
                                           List<CheckValidDTO> history,
                                           CheckValidDTO curr) {

        CheckValidDTO prev = history.isEmpty() ? null : history.getFirst();

        Long prevSeq = (prev != null) ? prev.seq() : null;
        Long currSeq = curr.seq();

        Long prevFirstSeq = (prev != null) ? num(prev.segFirstUri()) : null;
        Long prevLastSeq = (prev != null) ? num(prev.segLastUri()) : null;
        Long currFirstSeq = num(curr.segFirstUri());
        Long currLastSeq = num(curr.segLastUri());

        Long expectedFirstSeq = null;
        String detailReason;

        switch (result) {
            case WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL -> {
                long seqDiff = (prevSeq != null) ? (currSeq - prevSeq) : 0L;
                detailReason = String.format(
                        "seq rolled (prevSeq=%d, currSeq=%d, diff=%d) but segment window is identical: " +
                                "segmentCount=%d, firstUri=%s, lastUri=%s, tail=%s",
                        prevSeq, currSeq, seqDiff,
                        curr.segmentCount(),
                        curr.segFirstUri(),
                        curr.segLastUri(),
                        String.join(",", curr.tailUris())
                );
            }
            case WARN_SEGMENTS_CHANGED_SEQ_STUCK -> {
                detailReason = String.format(
                        "media-sequence stuck at %d but last segment changed (prevLast=%s, currLast=%s)",
                        currSeq,
                        prev != null ? prev.segLastUri() : "N/A",
                        curr.segLastUri()
                );
            }
            case ERROR_STALL_NO_PROGRESS -> {
                // 같은 seq 가 몇 번 연속으로 관측되었는지 재계산
                int sameCount = 1;
                for (CheckValidDTO dto : history) {
                    if (dto.seq() == currSeq) sameCount++;
                    else break;
                }
                detailReason = String.format(
                        "media-sequence stuck at %d for %d consecutive snapshots with no segment change",
                        currSeq, sameCount
                );
            }
            case WARN_SEQUENCE_CHANGE_TOO_FAR -> {
                long diff = (prevSeq != null) ? (currSeq - prevSeq) : 0L;
                detailReason = String.format(
                        "media-sequence jumped too far (prevSeq=%d, currSeq=%d, diff=%d >= 5)",
                        prevSeq, currSeq, diff
                );
            }
            case WARN_MEDIA_SEQUENCE_SEGMENT_MISMATCH -> {
                long seqDiff = (prevSeq != null) ? (currSeq - prevSeq) : 0L;
                if (prevFirstSeq != null && currFirstSeq != null) {
                    expectedFirstSeq = prevFirstSeq + seqDiff;
                }
                detailReason = String.format(
                        "media-sequence and segment numbers mismatch: " +
                                "prevSeq=%d, currSeq=%d, seqDiff=%d, " +
                                "prevFirstSeq=%s, currFirstSeq=%s, prevLastSeq=%s, currLastSeq=%s, expectedFirstSeq=%s",
                        prevSeq, currSeq, seqDiff,
                        String.valueOf(prevFirstSeq),
                        String.valueOf(currFirstSeq),
                        String.valueOf(prevLastSeq),
                        String.valueOf(currLastSeq),
                        String.valueOf(expectedFirstSeq)
                );
            }
            case ERROR_SEQ_REWIND -> {
                long diff = (prevSeq != null) ? (currSeq - prevSeq) : 0L;
                detailReason = String.format(
                        "media-sequence rewound: prevSeq=%d, currSeq=%d, diff=%d (negative)",
                        prevSeq, currSeq, diff
                );
            }
            default -> {
                // 다른 룰 (혹시 추가될 경우)
                detailReason = String.format(
                        "validation rule %s triggered for traceId=%s, resolution=%s",
                        result.name(), curr.traceId(), curr.resolution()
                );
            }
        }

        // requestDurationMs: Duration -> Long(ms) 변환
        Long requestDurationMs = curr.requestDurationMs() != null
                ? curr.requestDurationMs().toMillis()
                : null;

        // discontinuityPos, tailUris 는 그대로 List 로 보냄
        return new SaveFailureDTO(
                curr.traceId(),
                result.name(),          // ruleCode
                detailReason,           // 사람이 읽을 설명
                curr.resolution(),
                curr.tsEpochMs(),
                requestDurationMs,
                curr.seq(),
                curr.dseq(),
                curr.discontinuityPos(),
                curr.segmentCount(),
                curr.hashNorm(),
                curr.segFirstUri(),
                curr.segLastUri(),
                curr.tailUris(),
                curr.wrongExtinf(),
                prevSeq,
                prevLastSeq,
                currFirstSeq,
                expectedFirstSeq
        );
    }
}