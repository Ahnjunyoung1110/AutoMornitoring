package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.domain.api.service.StatusService;
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

    private final StatusService statusService;

    @GetMapping("/{traceId}")
    public ResponseEntity<String> getTraceIdStatus(@PathVariable String traceId) {
        String status = statusService.getTraceIdStatus(traceId);
        if (status == null || status.equals("false")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{traceId}/details")
    public ResponseEntity<Map<String, String>> getAllStatuses(@PathVariable String traceId) {
        Map<String, String> statuses = statusService.getAllStatusesForTraceId(traceId);
        if (statuses.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(statuses);
    }
}
