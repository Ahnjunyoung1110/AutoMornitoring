package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.contract.program.ProgramOptionCommand;
import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.client.ProgramClient;
import AutoMonitoring.AutoMonitoring.domain.api.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/arguments")
public class ArgumentsController {
    private final RecordManifest recordManifest;
    private final ProgramService programService;
    private final ProgramClient programClient;

    @PatchMapping("/setOptions")
    public ResponseEntity<?> setOptions(@Valid @RequestBody ProgramOptionCommand programOptionCommand){

        programService.setOptions(programOptionCommand);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/getOptions/{traceId}")
    public ResponseEntity<?> getOptions(@PathVariable String traceId){
        return programClient.getSaveState(traceId);
    }

}
