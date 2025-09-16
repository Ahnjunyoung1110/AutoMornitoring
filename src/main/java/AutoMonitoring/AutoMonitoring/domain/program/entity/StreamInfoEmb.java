package AutoMonitoring.AutoMonitoring.domain.program.entity;


import AutoMonitoring.AutoMonitoring.domain.program.dto.StreamDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class StreamInfoEmb {

    @Column(name = "type", length = 16)   private String type;     // "video"|"audio"
    @Column(name = "codec", length = 32)  private String codec;    // h264, aac, hevc...
    @Column(name = "width")               private Integer width;   // video
    @Column(name = "height")              private Integer height;  // video
    @Column(name = "fps")                 private Double fps;      // video
    @Column(name = "channels")            private Integer channels;// audio
    @Column(name = "lang", length = 16)   private String lang;     // 선택

    public static StreamInfoEmb fromDto(StreamDTO d) {
        return StreamInfoEmb.builder()
                .type(d.type()).codec(d.codec())
                .width(d.width()).height(d.height())
                .fps(d.fps()).channels(d.channels()).lang(d.lang())
                .build();
    }
    public StreamDTO toDto() {
        return StreamDTO.builder()
                .type(type).codec(codec).width(width).height(height)
                .fps(fps).channels(channels).lang(lang)
                .build();
    }
}
