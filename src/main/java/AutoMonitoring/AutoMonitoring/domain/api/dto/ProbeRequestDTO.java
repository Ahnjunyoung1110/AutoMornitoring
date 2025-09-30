package AutoMonitoring.AutoMonitoring.domain.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProbeRequestDTO {
    String url;
    String UserAgent;
}
