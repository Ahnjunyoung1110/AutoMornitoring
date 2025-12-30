package AutoMonitoring.AutoMonitoring.domain.api.client;

import AutoMonitoring.AutoMonitoring.contract.program.ProgramInformation;
import AutoMonitoring.AutoMonitoring.contract.program.SaveM3u8State;
import AutoMonitoring.AutoMonitoring.domain.api.dto.SystemConfigRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "program-feign-client",
        url = "${program-service.url}"
)
public interface ProgramClient {

    @GetMapping("/api/program/information/{traceId}")
    ResponseEntity<Map<String, List<ProgramInformation>>> getInformationByTraceId(@PathVariable("traceId") String traceId);

    @GetMapping("/api/program/saveState/{traceId}")
    ResponseEntity<SaveM3u8State> getSaveState(@PathVariable("traceId") String traceId);

    @GetMapping("/api/program/config/current")
    ResponseEntity<SystemConfigRequest> getCurrentConfigByFeign();

}
