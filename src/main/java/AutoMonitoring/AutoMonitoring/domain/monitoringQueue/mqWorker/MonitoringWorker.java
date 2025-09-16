package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.dto.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Component
@Slf4j
public class MonitoringWorker {

    private final ParseMediaManifest parseMediaManifest;
    private final GetMediaService getMediaService;
    private final RabbitTemplate rabbit;
    private final RedisMediaService redisMediaService;
    private final MonitoringService monitoringService;


    // DB를 저장한 후 모니터링을 시작하는 함수
    @RabbitListener(queues = RabbitNames.Q_STAGE3)
    void startMessage(ProgramInfo info){

        for(String key: info.getResolutionToUrl().keySet()){
            StartMonitoringDTO dto = new StartMonitoringDTO(info.getTraceId(), info.getResolutionToUrl().get(key) , key, info.getUserAgent());
            monitoringService.startMornitoring(dto);
        }

        log.info("모니터링을 시작합니다. %s,%s".formatted(info.getTraceId(),info.getResolutionToUrl()));
    }



    // 내부에서 계속 돌아가는 메시지를 받아 처리하는 함수, 처리에 실패하면 딜레이 큐로 보낸다.
    @RabbitListener(queues = RabbitNames.DELAY_STAGE1)
    void receiveMessage(CheckMediaManifestCmd cmd){
        Instant nowTime = Instant.now();

        // curl
        String media = getMediaService.getMedia(cmd.mediaUrl(), cmd.userAgent());

        Instant getTime = Instant.now();
        Duration requestDurationMs = Duration.between(nowTime,getTime);

        // 파싱
        RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(media, requestDurationMs,cmd.traceId(), cmd.resolution());



        // redis에 저장
        addToRedis(cmd.traceId(),cmd.resolution(),recordMediaToRedisDTO);

        CheckMediaManifestCmd newCmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(),cmd.userAgent() ,0, nowTime, cmd.traceId());

        Long delay = 5L - (Duration.between(cmd.publishTime(), nowTime).minusSeconds(5)).toMillis();

        // validate test 메시지 발행
        sendMessageToValid(cmd.traceId(),cmd.resolution());
        // 딜레이(5초 - 이전시간 걸린 딜레이) 메시지 생성 후 저장
        rabbit.convertAndSend(RabbitNames.DELAY_PIPELINE, RabbitNames.DRK_STAGE1, newCmd,
                m -> {m.getMessageProperties().setDelayLong(delay); return m; }
        );
    }

    // redis에 메니페스트를 저장
    void addToRedis(String traceId, String resolution ,RecordMediaToRedisDTO recordMediaToRedisDTO){
        redisMediaService.pushHistory(traceId, resolution, recordMediaToRedisDTO, 10);
        redisMediaService.saveState(traceId, resolution, recordMediaToRedisDTO);
    }

    // validate 테스트를 위해 메시지를 전달
    void sendMessageToValid(String traceId, String resolution){
        CheckValidDTO send = new CheckValidDTO(traceId, resolution);
        rabbit.convertAndSend(RabbitNames.EX_PIPELINE, RabbitNames.RK_VALID, send);
    }
}