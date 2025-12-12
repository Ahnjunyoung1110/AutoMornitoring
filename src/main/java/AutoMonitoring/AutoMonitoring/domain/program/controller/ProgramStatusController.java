package AutoMonitoring.AutoMonitoring.domain.program.controller;

import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/program/status")
@RequiredArgsConstructor
public class ProgramStatusController {

    private final ProgramService programService;

    @GetMapping("/{traceId}")
    public ResponseEntity<Map<String, String>> getStatusByTraceId(@PathVariable String traceId) {
        Map<String, String> status = programService.getStatuesByTraceId(traceId);
        if (status == null || status.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
