package AutoMonitoring.AutoMonitoring.domain.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(
        name = "program-status-service",
        url = "${program-service.url}"
)
public interface StatusClient {

    @GetMapping("/api/program/status/{traceId}")
    ResponseEntity<Map<String, String>> getStatusByTraceId(@PathVariable("traceId") String traceId);

}
