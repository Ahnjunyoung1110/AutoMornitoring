package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.program.UpdateSystemConfigCommand;
import AutoMonitoring.AutoMonitoring.domain.api.dto.SystemConfigRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ConfigControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/config/update는 시스템 설정 요청을 받아 RabbitMQ 큐로 메시지를 전송해야 한다")
    void updateSystemConfig_shouldSendMessageToProgramCommandQueue() throws Exception {
        // given: 테스트용 시스템 설정 요청 데이터
        SystemConfigRequest request = new SystemConfigRequest(
                true, 10, 300, 5,
                20, 5000, 1000,
                true, true
        );

        // when: /api/config/update 엔드포인트로 POST 요청을 보냄
        mockMvc.perform(MockMvcRequestBuilders.post("/api/config/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("설정 업데이트 요청이 성공적으로 전송되었습니다."));

        // then: RabbitMQ의 'queue.program.command' 큐에서 메시지를 수신하여 검증
        Object receivedObject = rabbitTemplate.receiveAndConvert(RabbitNames.Q_PROGRAM_COMMAND, 5000);

        assertThat(receivedObject).isNotNull();
        assertThat(receivedObject).isInstanceOf(UpdateSystemConfigCommand.class);

        UpdateSystemConfigCommand receivedCommand = (UpdateSystemConfigCommand) receivedObject;

        // 요청 데이터와 수신된 커맨드 데이터가 일치하는지 확인
        assertThat(receivedCommand.alarmEnabled()).isEqualTo(request.alarmEnabled());
        assertThat(receivedCommand.threshold()).isEqualTo(request.threshold());
        assertThat(receivedCommand.alarmCooldownSeconds()).isEqualTo(request.alarmCooldownSeconds());
        assertThat(receivedCommand.reconnectThreshold()).isEqualTo(request.reconnectThreshold());
        assertThat(receivedCommand.reconnectTimeoutMillis()).isEqualTo(request.reconnectTimeoutMillis());
        assertThat(receivedCommand.reconnectRetryDelayMillis()).isEqualTo(request.reconnectRetryDelayMillis());
        assertThat(receivedCommand.httpRequestTimeoutMillis()).isEqualTo(request.httpRequestTimeoutMillis());
        assertThat(receivedCommand.autoRefresh()).isEqualTo(request.autoRefresh());
        assertThat(receivedCommand.monitoringEnabled()).isEqualTo(request.monitoringEnabled());
    }
}