package AutoMonitoring.AutoMonitoring.domain.api.controller;


import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import AutoMonitoring.AutoMonitoring.domain.api.dto.MonitoringProgramDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(
        name = "program-service",
        url = "${program-service.url}" // or 서비스 디스커버리 사용 시 이름만
)
public interface DashBoardController {

    @GetMapping("/api/programs/monitoring")
    Page<MonitoringProgramDTO> getMonitoringPrograms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String channelId,
            @RequestParam(required = false) String tp,
            @RequestParam(required = false) ResolutionStatus resolutionStatus,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
            );

}
