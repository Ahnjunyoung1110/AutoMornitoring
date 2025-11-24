package AutoMonitoring.AutoMonitoring.domain.program.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;

/**
 * M3U8 유효성 검사 실패 이력을 기록하는 엔티티
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

    /**
     * 어떤 Program의 유효성 검사 로그인지 나타내는 관계 필드
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /**
     * 유효성 검사를 수행한 시간
     */
    @Column(nullable = false)
    private Instant validatedAt;

    /**
     * 유효성 검사 실패 사유
     */
    private String reason;

    // --- Fields from CheckValidDTO ---
    @Column(length = 32)
    private String resolution;
    private Instant tsEpochMs;  // 수집시각
    private Long requestDurationMs; // Duration 대신 Long으로 저장 (ms)
    private long seq;           // media-sequence의 값
    private long dseq;          // #discontinuity sequence의 값
    
    @Column(length = 1024) // 콤마로 구분된 문자열로 저장 (List<Integer>)
    private String discontinuityPos;  // 해당 m3u8에 #EXT-X-discontinuity 가 몇번쨰 uri 앞에 있는가
    
    private int segmentCount; // 몇개의 청크가 입력되어있는가
    
    @Column(length = 256) // Hash 값 저장
    private String hashNorm;      // 설정이 바뀌지는 않았는지를 확인하기 위한 정규화 후 hash값
    
    @Column(length = 2048) // URI 값 저장
    private String segFirstUri;    // 첫 세그먼트 URI (쿼리X)
    
    @Column(length = 2048) // URI 값 저장
    private String segLastUri;       // 마지막 세그먼트 URI (쿼리X)
    
    @Column(length = 4096) // 콤마로 구분된 문자열로 저장 (List<String>)
    private String tailUris; // ← ["seg123.ts","seg124.ts","seg125.ts"]의 마지막 3개
    
    private boolean wrongExtinf; // EXTINF 가 5가 아닌데 이후에 #EXT-X-DISCONTINUITY 가 등장하지 않은경우 true

}
