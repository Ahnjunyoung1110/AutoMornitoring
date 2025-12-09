package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.contract.program.ProgramOptionCommand;
import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/arguments")
public class SetArgumentsController {
    private final RecordManifest recordManifest;
    private final ProgramService programService;

    @PostMapping("/setOptions")
    public ResponseEntity<?> setOptions(@Valid @RequestBody ProgramOptionCommand programOptionCommand){

        programService.setOptions(programOptionCommand);

        return ResponseEntity.ok().build();
    }

}
