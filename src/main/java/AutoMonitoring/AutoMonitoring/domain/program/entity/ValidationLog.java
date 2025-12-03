package AutoMonitoring.AutoMonitoring.domain.program.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;

/**
 * M3U8 유효성 검사 로그 엔티티
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValidationLog {

    @Id
    @UuidGenerator
    private String id;

    /** 어떤 Program의 유효성 검사인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** 유효성 검사 시각 */
    @Column(nullable = false)
    private Instant validatedAt;

    /**
     * 룰 코드
     * 예: ERROR_MEDIA_SEQUENCE_SEGMENT_MISMATCH, WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL
     */
    @Column(length = 128, nullable = false)
    private String ruleCode;

    /**
     * 사람이 읽기 위한 상세 reason (prev/current 비교, 기대값 등)
     */
    @Column(length = 2048)
    private String detailReason;

    // ---- Fields from CheckValidDTO ----

    @Column(length = 32)
    private String resolution;

    private Instant tsEpochMs;            // 수집 시각
    private Long requestDurationMs;       // 다운로드 소요 시간
    private long seq;                     // media-sequence
    private long dseq;                    // discontinuity-sequence

    @Column(length = 1024)
    private String discontinuityPos;      // "#EXT-X-DISCONTINUITY" 위치 목록

    private int segmentCount;

    @Column(length = 256)
    private String hashNorm;              // 정규화 후 hash

    @Column(length = 2048)
    private String segFirstUri;

    @Column(length = 2048)
    private String segLastUri;

    @Column(length = 4096)
    private String tailUris;              // 마지막 3개 URI

    private boolean wrongExtinf;

    // ---- Added for comparison/diagnostics ----

    /** 직전 media-sequence */
    private Long prevSeq;

    /** 직전 윈도우의 마지막 세그먼트 번호 */
    private Long prevLastSegmentSeq;

    /** 이번 윈도우의 첫 세그먼트 번호 */
    private Long currFirstSegmentSeq;

    /** 룰이 계산한 기대되는 첫 세그먼트 번호 */
    private Long expectedFirstSegmentSeq;
}
