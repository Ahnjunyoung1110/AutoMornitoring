package AutoMonitoring.AutoMonitoring.contract.program;

import lombok.Builder;
import org.springframework.data.domain.Sort;

@Builder
public record PageableDTO(
        int page,
        int size,
        String sortProperty, // e.g., "id", "traceId"
        Sort.Direction sortDirection // e.g., "ASC", "DESC"
) {}
