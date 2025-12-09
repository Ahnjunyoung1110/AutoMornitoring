package AutoMonitoring.AutoMonitoring.domain.api.dto;

import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import lombok.Builder;

@Builder
public record MonitoringProgramDTO(
    String traceId,
    String masterManifestUrl,
    String channelName,
    String channelId,
    String tp,
    ResolutionStatus status
) {}
