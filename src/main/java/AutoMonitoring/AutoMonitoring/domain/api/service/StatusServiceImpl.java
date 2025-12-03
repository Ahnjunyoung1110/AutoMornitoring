package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.program.DbGetStatusCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {

    private final RabbitTemplate rabbit;

    @Override
    public Map<String, String> getAllStatusesForTraceId(String traceId) {
        return rabbit.convertSendAndReceiveAsType(
                RabbitNames.EX_PROVISIONING,
                RabbitNames.RK_STAGE2,
                new DbGetStatusCommand(traceId),
                new ParameterizedTypeReference<TreeMap<String, String>>() {
                }
        );

    }
}
