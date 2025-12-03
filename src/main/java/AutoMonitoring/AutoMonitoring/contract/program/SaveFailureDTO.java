package AutoMonitoring.AutoMonitoring.contract.program;

import java.time.Instant;
import java.util.List;

/**
 * Program 유효성 검사 실패(또는 경고) 한 건을
 * Program 서비스 쪽으로 전달하기 위한 DTO.
 *
 * ValidationLog 엔티티에 매핑될 수 있도록 설계.
 */
public record SaveFailureDTO(

        // 어떤 Program 의 로그인지 식별 (Program 은 traceId 로 조회)
        String traceId,

        // 어떤 룰이 터졌는지 (ex. ERROR_MEDIA_SEQUENCE_SEGMENT_MISMATCH, WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL)
        String ruleCode,

        // 사람이 읽기 위한 상세 reason (prev/curr 비교 설명, tail 정보 등)
        String detailReason,

        // ---- 현재 m3u8 스냅샷 정보 (기존 CheckValidDTO 에서 오던 값들) ----
        String resolution,
        Instant tsEpochMs,
        Long requestDurationMs,
        long seq,
        long dseq,
        List<Integer> discontinuityPos,
        int segmentCount,
        String hashNorm,
        String segFirstUri,
        String segLastUri,
        List<String> tailUris,
        boolean wrongExtinf,

        // ---- 직전 윈도우 / 기대값 요약 (룰 판단에 쓰인 핵심 숫자만) ----
        Long prevSeq,
        Long prevLastSegmentSeq,
        Long currFirstSegmentSeq,
        Long expectedFirstSegmentSeq
) {}
