package AutoMonitoring.AutoMonitoring.domain.program.dto;

import java.time.Instant;
import java.util.List;


public record ProbeDTO(
        String traceId,
        Instant probeAt,
        String masterManifestUrl,
        String format,              // ex) "hls"
        Double durationSec,         // null 허용
        Integer overallBitrate,     // null 허용 (bps)
        List<StreamDTO> streams,    // 비어있으면 []
        List<VariantDTO> variants  // 비어있으면 []
) {}
