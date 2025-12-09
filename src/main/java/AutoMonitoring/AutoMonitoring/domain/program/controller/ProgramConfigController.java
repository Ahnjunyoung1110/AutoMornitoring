package AutoMonitoring.AutoMonitoring.domain.program.controller;


import AutoMonitoring.AutoMonitoring.domain.api.dto.SystemConfigRequest;
import AutoMonitoring.AutoMonitoring.domain.program.entity.SystemConfig;
import AutoMonitoring.AutoMonitoring.domain.program.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/program/config")
@RequiredArgsConstructor
public class ProgramConfigController {

    private final SystemConfigRepository configRepository;
    @GetMapping("/current")
    public ResponseEntity<SystemConfigRequest> getCurrentConfig(){
        SystemConfig config = configRepository.findById(1L).orElseThrow();

        SystemConfigRequest responseConfig = new SystemConfigRequest(config.isAlarmEnabled(), config.getThreshold(), config.getAlarmCooldownSeconds(), config.getReconnectThreshold(), config.getReconnectTimeoutMillis(), config.getReconnectRetryDelayMillis(), config.getHttpRequestTimeoutMillis(), config.isAutoRefresh(), config.isMonitoringEnabled());
        return ResponseEntity.ok(responseConfig);
    }
}
