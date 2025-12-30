package AutoMonitoring.AutoMonitoring.domain.program.controller;

import AutoMonitoring.AutoMonitoring.contract.program.SaveM3u8State;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/program/saveState")
@RequiredArgsConstructor
public class ProgramStateController {

    private final ProgramRepo programRepo;

    @GetMapping("/{traceId}")
    public ResponseEntity<SaveM3u8State> getSavedState(@PathVariable String traceId){
        SaveM3u8State state = programRepo.findByTraceId(traceId).orElseThrow().getSaveM3u8State();
        return ResponseEntity.ok().body(state);
    }
}
