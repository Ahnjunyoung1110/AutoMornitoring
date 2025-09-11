package AutoMonitoring.AutoMonitoring.domain.program.adapter;

import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.StreamInfoEmb;
import AutoMonitoring.AutoMonitoring.domain.program.entity.VariantInfoEmb;

import java.util.List;

public class ProgramBuilderSample {
    public static Program sample() {
        var streams = List.of(
                StreamInfoEmb.builder()
                        .type("video").codec("h264")
                        .width(1920).height(1080).fps(30.0)
                        .build(),
                StreamInfoEmb.builder()
                        .type("audio").codec("aac")
                        .channels(2).lang("eng")
                        .build()
        );

        var variants = List.of(
                VariantInfoEmb.builder()
                        .resolution("1920x1080").bandwidth(3_731_200)
                        .uri("https://cdn.example.com/live/1080/index.m3u8")
                        .audioGroup("aac-stereo")
                        .build(),
                VariantInfoEmb.builder()
                        .resolution("1280x720").bandwidth(1_900_800)
                        .uri("https://cdn.example.com/live/720/index.m3u8")
                        .audioGroup("aac-stereo")
                        .build()
        );

        return Program.builder()
                .traceId("TRACE-1234567890")
                .masterManifestUrl("https://live.example.com/master.m3u8?apikey=AAA&resolution=1920x1080")
                .format("hls")
                .durationSec(1234.56)
                .overallBitrate(3_500_000)
                .streams(streams)     // @Builder.Default 있어도, 이렇게 덮어쓰기 가능
                .variants(variants)
                .build();
    }
}
