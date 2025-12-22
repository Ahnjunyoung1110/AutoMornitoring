package AutoMonitoring.AutoMonitoring.domain.program.controller;

import AutoMonitoring.AutoMonitoring.contract.program.ProgramInformation;
import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/program/information")
@RequiredArgsConstructor
public class ProgramInformationController {

    private final ProgramService programService;

    @GetMapping("/{traceId}")
    public ResponseEntity<Map<String, List<ProgramInformation>>> getInformationByTraceId(@PathVariable String traceId) {
        Map<String, List<ProgramInformation>> status = programService.getInformationByTraceId(traceId);
        if (status == null || status.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
