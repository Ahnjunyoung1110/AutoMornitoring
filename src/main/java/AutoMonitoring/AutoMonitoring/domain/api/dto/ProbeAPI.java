package AutoMonitoring.AutoMonitoring.domain.api.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ProbeAPI(
        String traceId,
        String masterUrl
) {
}
