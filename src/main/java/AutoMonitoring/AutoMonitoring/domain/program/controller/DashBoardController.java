package AutoMonitoring.AutoMonitoring.domain.program.controller;

import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import AutoMonitoring.AutoMonitoring.domain.api.dto.MonitoringProgramDTO;
import AutoMonitoring.AutoMonitoring.domain.program.application.DashBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashBoardController {

    private final DashBoardService dashBoardService;

    @GetMapping("/monitoring")
    public Page<MonitoringProgramDTO> getMonitoringPrograms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String channelId,
            @RequestParam(required = false) String tp,
            @RequestParam(required = false) ResolutionStatus resolutionStatus,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        return dashBoardService.getMonitoringPrograms(resolutionStatus, traceId, channelId, tp, pageable);
    }
}
