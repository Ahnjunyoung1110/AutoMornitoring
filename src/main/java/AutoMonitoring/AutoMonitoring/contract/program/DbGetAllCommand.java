package AutoMonitoring.AutoMonitoring.contract.program;

import org.springframework.data.domain.Pageable;
public record DbGetAllCommand(
        String traceId,
        String tp,
        String channelId,
        ResolutionStatus status,
        Pageable pageable
) implements DbGetCommand{
}
