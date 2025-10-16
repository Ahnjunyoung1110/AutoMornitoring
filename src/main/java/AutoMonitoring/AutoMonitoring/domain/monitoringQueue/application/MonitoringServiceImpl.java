package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.api.service.UrlValidateCheck;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StopMornitoringDTO;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringServiceImpl implements MonitoringService {

    private final RabbitTemplate rabit;

    private final RedisService redis;

    private final UrlValidateCheck urlValidateCheck;

    @Override
    public void startMornitoring(StartMonitoringDTO dto) {

        // queue 에 넣을 dto 생성
        CheckMediaManifestCmd cmd = new CheckMediaManifestCmd(dto.manifestUrl(), dto.resolution( ),dto.userAgent(), 0, Instant.now(), dto.traceId());

        String key = RedisKeys.queueFlag(dto.traceId(), dto.resolution());
        Duration ttl = Duration.ofMinutes(3);

        boolean isValidUrl = urlValidateCheck.check(dto.manifestUrl());
        if(!isValidUrl){
            log.info("URL이 유효하지 않습니다." + dto.manifestUrl());
            String stateKey = RedisKeys.state(dto.traceId(), dto.resolution());
            redis.setValues(stateKey, "WRONG_URL");
            throw new AmqpRejectAndDontRequeueException("Wrong sub Url");
        }
        boolean first = redis.getOpsAbsent(key, "1", ttl);



        if(first){
            // queue에 입력
            rabit.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK, cmd);
            log.info("모니터링을 시작합니다." + cmd.traceId() + " " + cmd.resolution() + " " + cmd.userAgent());
        }
        else{
            log.info("이미 모니터링을 수행중입니다.");
        }
    }

    // 추후 구현
    @Override
    public void stopMornitoring(StopMornitoringDTO stopMornitoringDTO) {

    }
}
