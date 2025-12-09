package AutoMonitoring.AutoMonitoring.domain.api.client;

import AutoMonitoring.AutoMonitoring.domain.api.dto.SystemConfigRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "program-config-service",
        url = "${program-service.url}"
)
public interface ConfigClient {
    @GetMapping("/api/program/config/current")
    ResponseEntity<SystemConfigRequest> getCurrentConfigByFeign();
}
