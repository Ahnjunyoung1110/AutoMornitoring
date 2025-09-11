package AutoMonitoring.AutoMonitoring.domain.program.entity;

import AutoMonitoring.AutoMonitoring.domain.program.dto.ProbeDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Program {
    @Id
    @UuidGenerator
    String id;

    /** 작업 단위 식별자 (DTO.traceId와 1:1) */
    @Column(name = "trace_id", nullable = false, unique = true, length = 64)
    private String traceId;

    private String masterManifestUrl;

    @Column(name = "format")
    private String format;

    @Column(name = "duration_sec")
    private Double durationSec;            // null 허용

    @Column(name = "overall_bitrate")
    private Integer overallBitrate;        // null 허용 (bps)

    /** streams */
    @ElementCollection
    @CollectionTable(name = "probe_streams", joinColumns = @JoinColumn(name = "probe_id"))
    @Builder.Default
    private List<StreamInfoEmb> streams = new ArrayList<>();

    /** variants */
    @ElementCollection
    @CollectionTable(name = "probe_variants", joinColumns = @JoinColumn(name = "probe_id"))
    @Builder.Default
    private List<VariantInfoEmb> variants = new ArrayList<>();



    /* 도메인 함수 */
//    public Program update()


    /* ---------- 매핑 헬퍼 ---------- */

    public static Program fromDto(ProbeDTO dto) {
        var b = Program.builder()
                .masterManifestUrl(dto.masterManifestUrl())
                .traceId(dto.traceId())
                .format(dto.format())
                .durationSec(dto.durationSec())
                .overallBitrate(dto.overallBitrate());

        List<StreamInfoEmb> sList = new ArrayList<>();
        if (dto.streams()!=null) dto.streams().forEach(s -> sList.add(StreamInfoEmb.fromDto(s)));

        List<VariantInfoEmb> vList = new ArrayList<>();
        if (dto.variants()!=null) dto.variants().forEach(v -> vList.add(VariantInfoEmb.fromDto(v)));

        b.streams(sList);
        b.variants(vList);
        return b.build();
    }

}
