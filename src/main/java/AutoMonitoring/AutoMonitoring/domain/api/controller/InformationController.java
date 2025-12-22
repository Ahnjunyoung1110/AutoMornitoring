package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.contract.program.ProgramInformation;
import AutoMonitoring.AutoMonitoring.domain.api.client.ProgramClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/information")
public class InformationController {

    private final ProgramClient programClient;

    @GetMapping("/{traceId}")
    public ResponseEntity<Map<String, List<ProgramInformation>>> getTraceIdInformation(@PathVariable String traceId) {
        ResponseEntity<Map<String, List<ProgramInformation>>> response = programClient.getInformationByTraceId(traceId);
        if (response.getBody() == null) {
            return ResponseEntity.notFound().build();
        }
        return response;
    }
}
