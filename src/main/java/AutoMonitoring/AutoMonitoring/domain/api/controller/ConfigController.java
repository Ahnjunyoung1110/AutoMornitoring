package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.program.UpdateSystemConfigCommand;
import AutoMonitoring.AutoMonitoring.domain.api.client.ProgramClient;
import AutoMonitoring.AutoMonitoring.domain.api.dto.SystemConfigRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 시스템 전반의 설정을 업데이트하는 API 엔드포인트를 제공하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final RabbitTemplate rabbitTemplate;
    private final ProgramClient programClient;

    /**
     * 시스템의 기본 설정값을 반환합니다.
     * 이 값들은 각 도메인의 ConfigHolder에 하드코딩된 초기값입니다.
     * @return 시스템 기본 설정 DTO
     */
    @GetMapping("/defaults")
    public ResponseEntity<SystemConfigRequest> getSystemConfigDefaults() {
        SystemConfigRequest defaults = new SystemConfigRequest(
                true,   // alarmEnabled from CheckValidConfigHolder
                5,      // threshold from CheckValidConfigHolder
                3600,     // alarmCooldownSeconds from CheckValidConfigHolder
                10,     // reconnectThreshold from MonitoringConfigHolder
                5000,   // reconnectTimeoutMillis from MonitoringConfigHolder
                2000,   // reconnectRetryDelayMillis from MonitoringConfigHolder
                5000,   // httpRequestTimeoutMillis from MonitoringConfigHolder
                true,  // autoRefresh from MonitoringConfigHolder
                true    // monitoringEnabled from MonitoringConfigHolder
        );
        return ResponseEntity.ok(defaults);
    }

    /**
     * 시스템 설정을 업데이트하고, 관련 메시지를 RabbitMQ를 통해 Program 도메인으로 전송합니다.
     * Program 도메인은 이 메시지를 받아 필요한 각 도메인(monitoringQueue, checkMediaValid)으로 설정을 전파합니다.
     * @param request 시스템 설정 요청 DTO
     * @return 처리 결과 메시지
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateSystemConfig(@RequestBody SystemConfigRequest request) {
        log.info("시스템 설정 업데이트 요청을 받았습니다: {}", request);

        // Convert DTO to Command
        UpdateSystemConfigCommand command = new UpdateSystemConfigCommand(
                request.alarmEnabled(),
                request.threshold(),
                request.alarmCooldownSeconds(),
                request.reconnectThreshold(),
                request.reconnectTimeoutMillis(),
                request.reconnectRetryDelayMillis(),
                request.httpRequestTimeoutMillis(),
                request.autoRefresh(),
                request.monitoringEnabled()
        );

        // Program 도메인 큐로 커맨드를 전송합니다.
        rabbitTemplate.convertAndSend(RabbitNames.EX_PROGRAM_COMMAND, RabbitNames.RK_PROGRAM_COMMAND, command);

        log.info("Program 도메인으로 UpdateSystemConfigCommand를 전송했습니다.");

        return ResponseEntity.ok("설정 업데이트 요청이 성공적으로 전송되었습니다.");
    }

    @GetMapping("/current")
    public ResponseEntity<SystemConfigRequest> getSystemConfigCurrent(){
        return programClient.getCurrentConfigByFeign();
    }
}
