package AutoMonitoring.AutoMonitoring.domain.program.entity;

import AutoMonitoring.AutoMonitoring.contract.program.ProbeDTO;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramOptionCommand;
import AutoMonitoring.AutoMonitoring.contract.program.SaveM3u8State;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.*;

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

    @Column(columnDefinition = "LONGTEXT")
    private String masterManifestUrl;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "tp")
    private String tp;

    @Column(name = "format")
    private String format;

    @Column(name = "duration_sec")
    private Double durationSec;            // null 허용

    @Column(name = "overall_bitrate")
    private Integer overallBitrate;        // null 허용 (bps)

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String UserAgent;

    @Column(name= "save_m3u8_state")
    @Enumerated(EnumType.STRING)
    private SaveM3u8State saveM3u8State;

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


    @PrePersist
    public void prePersist() {
        if (saveM3u8State == null){
            saveM3u8State = SaveM3u8State.WITHOUT_ADSLATE;
        }
    }


    /* 도메인 함수 */
    public static Map<String,String> getResolutionToUrlDomain(Program program){
        Map<String, String> resolutionToUrlMap = new HashMap<>();

        for(VariantInfoEmb variant : program.variants){
           resolutionToUrlMap.put(variant.getResolution(), variant.getUri());
        }
        return resolutionToUrlMap;
    }

    public Optional<VariantInfoEmb> findVariantByResolution(String resolution) {
        return variants.stream()
                .filter(v -> resolution.equals(v.getResolution()))
                .findFirst();
    }


    // 모니터링 옵션 적용하는 함수
    public void applyOption(ProgramOptionCommand command){
        if (command.saveM3u8State() != null) {
            this.saveM3u8State = command.saveM3u8State();
        }
    }

    // 업데이트 함수
    public void update(Program program){
        this.masterManifestUrl = program.getMasterManifestUrl();
        this.format = program.getFormat();
        this.durationSec = program.getDurationSec();
        this.overallBitrate = program.getOverallBitrate();
        this.UserAgent = program.getUserAgent();
        this.streams = program.getStreams();
        this.variants = program.getVariants();
        this.channelName = program.getChannelName();
        this.channelId = program.getChannelId();
        this.tp = program.getTp();
    }
    /* ---------- 매핑 헬퍼 ---------- */
    public static Program fromDto(ProbeDTO dto) {
        var b = Program.builder()
                .masterManifestUrl(dto.masterManifestUrl())
                .traceId(dto.traceId())
                .channelName(dto.channelName())
                .channelId(dto.channelId())
                .tp(dto.tp())
                .format(dto.format())
                .durationSec(dto.durationSec())
                .UserAgent(dto.userAgent())
                .saveM3u8State(dto.saveM3u8State())
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
