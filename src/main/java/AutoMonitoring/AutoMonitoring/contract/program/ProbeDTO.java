package AutoMonitoring.AutoMonitoring.contract.program;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ProbeDTO(
        String traceId,
        Instant probeAt,
        String masterManifestUrl,
        String userAgent,
        String format,              // ex) "hls"
        Double durationSec,         // null 허용
        Integer overallBitrate,     // null 허용 (bps)
        SaveM3u8State saveM3u8State,    // null 허용(기본 WITHOUT_ADSLATE)
        List<StreamDTO> streams,    // 비어있으면 []
        List<VariantDTO> variants  // 비어있으면 []
) {}
