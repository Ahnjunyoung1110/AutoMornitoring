package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.domain.api.client.StatusClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/status")
public class StatusController {

    private final StatusClient statusClient;

    @GetMapping("/{traceId}")
    public ResponseEntity<Map<String, String>> getTraceIdStatus(@PathVariable String traceId) {
        ResponseEntity<Map<String, String>> response = statusClient.getStatusByTraceId(traceId);
        if (response.getBody() == null) {
            return ResponseEntity.notFound().build();
        }
        return response;
    }
}
