package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StopMornitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private final RabbitTemplate rabit;

    private final RedisService redis;

    @Override
    public void startMornitoring(StartMonitoringDTO dto) {

        // queue 에 넣을 dto 생성
        CheckMediaManifestCmd cmd = new CheckMediaManifestCmd(dto.manifestUrl(), 0, Instant.now(), dto.traceId());

        // queue에 입력
        rabit.convertAndSend(RabbitNames.DELAY_PIPELINE, RabbitNames.DRK_STAGE1, cmd);
        // redis 변경
        redis.setValues(dto.traceId(), "MONITORING");
        redis.setValues(dto.manifestUrl(), "MONITORING");
    }

    // 추후 구현
    @Override
    public void stopMornitoring(StopMornitoringDTO stopMornitoringDTO) {

    }
}
