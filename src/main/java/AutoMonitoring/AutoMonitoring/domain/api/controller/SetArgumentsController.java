package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/arguments")
public class SetArgumentsController {
    private final RecordManifest recordManifest;

    @PostMapping("/record_discontinuity")
    public ResponseEntity<?> recordDiscontinuity(@RequestParam String traceId, @RequestParam Boolean setValue, @RequestParam String userAgent){

        recordManifest.recordAdLog(traceId, setValue);

        return ResponseEntity.ok().build();
    }
}
