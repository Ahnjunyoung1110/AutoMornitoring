package AutoMonitoring.AutoMonitoring.contract.program;

import lombok.Builder;

import java.util.List;

@Builder
public record ProbeDTO(
        String traceId,
        String masterManifestUrl,
        String channelName,
        String channelId,
        String tp,
        String userAgent,
        SaveM3u8State saveM3u8State,
        String format,
        Double durationSec,
        Integer overallBitrate,
        List<StreamDTO> streams,
        List<VariantDTO> variants
) {
}
