package AutoMonitoring.AutoMonitoring.domain.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class VariantInfoEmb {
    @Column(name = "resolution", length = 32) private String resolution; // "1920x1080"
    @Column(name = "bandwidth")               private Integer bandwidth; // bps
    @Column(name = "uri", length = 2048)      private String uri;        // 절대 URI 권장
    @Column(name = "audio_group", length = 64)private String audioGroup; // 선택

    public static VariantInfoEmb fromDto(AutoMonitoring.AutoMonitoring.domain.dto.VariantDTO d) {
        return VariantInfoEmb.builder()
                .resolution(d.resolution()).bandwidth(d.bandwidth())
                .uri(d.uri()).audioGroup(d.audioGroup())
                .build();
    }
    public AutoMonitoring.AutoMonitoring.domain.dto.VariantDTO toDto() {
        return new AutoMonitoring.AutoMonitoring.domain.dto.VariantDTO(resolution, bandwidth, uri, audioGroup);
    }
}
