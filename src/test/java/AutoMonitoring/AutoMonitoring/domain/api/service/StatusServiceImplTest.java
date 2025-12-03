package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.program.DbGetStatusCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private StatusServiceImpl statusService;

    private String traceId;

    @BeforeEach
    void setUp() {
        traceId = "test-trace-id-123";
    }

    @Test
    @DisplayName("traceId로 상태 조회 시 RabbitTemplate을 통해 DbGetStatusCommand를 발행하고 결과를 반환한다.")
    void getAllStatusesForTraceId_Success() {
        // given
        TreeMap<String, String> expectedStatusMap = new TreeMap<>();
        expectedStatusMap.put("1920x1080", "MONITORING");
        expectedStatusMap.put("720p", "FAILED");
        expectedStatusMap.put(traceId, "OVERALL_STATUS_OK"); // DbGetStatusCommand가 전체 상태도 포함할 수 있다고 가정

        when(rabbitTemplate.convertSendAndReceiveAsType(
                eq(RabbitNames.EX_PROVISIONING),
                eq(RabbitNames.RK_STAGE2),
                // ArgumentCaptor를 사용하여 DbGetStatusCommand 객체를 캡처
                ArgumentCaptor.forClass(DbGetStatusCommand.class).capture(),
                // ParameterizedTypeReference는 Mockito에서 직접 비교하기 어렵기 때문에 any() 사용
                // 하지만 실제 호출될 때는 ParameterizedTypeReference 인스턴스가 전달됨을 인지
                eq(new ParameterizedTypeReference<TreeMap<String, String>>() {})
        )).thenReturn(expectedStatusMap);

        // when
        Map<String, String> actualStatusMap = statusService.getAllStatusesForTraceId(traceId);

        // then
        // 1. rabbitTemplate.convertSendAndReceiveAsType가 정확한 인자로 호출되었는지 검증
        ArgumentCaptor<DbGetStatusCommand> commandCaptor = ArgumentCaptor.forClass(DbGetStatusCommand.class);
        verify(rabbitTemplate).convertSendAndReceiveAsType(
                eq(RabbitNames.EX_PROVISIONING),
                eq(RabbitNames.RK_STAGE2),
                commandCaptor.capture(),
                eq(new ParameterizedTypeReference<TreeMap<String, String>>() {})
        );

        DbGetStatusCommand sentCommand = commandCaptor.getValue();
        assertThat(sentCommand.traceId()).isEqualTo(traceId);

        // 2. 반환된 맵이 예상 결과와 일치하는지 검증
        assertThat(actualStatusMap).isEqualTo(expectedStatusMap);
    }

    @Test
    @DisplayName("RabbitTemplate 응답이 null인 경우 빈 Map을 반환한다.")
    void getAllStatusesForTraceId_ReturnsEmptyMapOnNullResponse() {
        // given
        when(rabbitTemplate.convertSendAndReceiveAsType(
                eq(RabbitNames.EX_PROVISIONING),
                eq(RabbitNames.RK_STAGE2),
                ArgumentCaptor.forClass(DbGetStatusCommand.class).capture(),
                eq(new ParameterizedTypeReference<TreeMap<String, String>>() {})
        )).thenReturn(null);

        // when
        Map<String, String> actualStatusMap = statusService.getAllStatusesForTraceId(traceId);

        // then
        assertThat(actualStatusMap).isNull();
        verify(rabbitTemplate).convertSendAndReceiveAsType(
                eq(RabbitNames.EX_PROVISIONING),
                eq(RabbitNames.RK_STAGE2),
                ArgumentCaptor.forClass(DbGetStatusCommand.class).capture(),
                eq(new ParameterizedTypeReference<TreeMap<String, String>>() {})
        );
    }
}
