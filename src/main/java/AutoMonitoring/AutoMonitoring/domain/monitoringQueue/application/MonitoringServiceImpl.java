package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramStatusCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import AutoMonitoring.AutoMonitoring.domain.api.service.UrlValidateCheck;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StopMornitoringDTO;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
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
        CheckMediaManifestCmd cmd = new CheckMediaManifestCmd(dto.manifestUrl(), dto.resolution( ),dto.userAgent(), 0, Instant.now(), dto.traceId(), dto.epoch());
        Duration ttl = Duration.ofMinutes(3);

        boolean isValidUrl = urlValidateCheck.check(dto.manifestUrl());
        if(!isValidUrl){
            log.info("URL이 유효하지 않습니다." + dto.manifestUrl());

            ProgramStatusCommand statusCmd = new ProgramStatusCommand(dto.traceId(), dto.resolution(), ResolutionStatus.WRONG_URL);
            rabit.convertAndSend(RabbitNames.EX_PROGRAM_COMMAND, RabbitNames.RK_PROGRAM_COMMAND, statusCmd);
            throw new AmqpRejectAndDontRequeueException("Wrong sub Url");
        }

        rabit.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK, cmd);
        log.info("모니터링을 시작합니다." + cmd.traceId() + " " + cmd.resolution() + " " + cmd.userAgent());

    }


    // 추후 구현
    @Override
    public void stopMornitoring(StopMornitoringDTO stopMornitoringDTO) {
    }
}
