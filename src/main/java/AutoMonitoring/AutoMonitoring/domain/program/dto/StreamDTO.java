package AutoMonitoring.AutoMonitoring.domain.program.dto;

import lombok.Builder;

@Builder
public record StreamDTO(
        String type,        // "video" | "audio"
        String codec,       // h264, aac, hevc...
        Integer width,      // video만
        Integer height,     // video만
        Double fps,         // video만 (null 허용)
        Integer channels,   // audio만
        String lang         // audio/자막 언어 (선택)
) {}
