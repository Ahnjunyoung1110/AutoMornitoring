package AutoMonitoring.AutoMonitoring.domain.api.dto;

import lombok.Builder;

@Builder
public record ProbeAPI(
        String traceId,
        String masterUrl
) {
}
