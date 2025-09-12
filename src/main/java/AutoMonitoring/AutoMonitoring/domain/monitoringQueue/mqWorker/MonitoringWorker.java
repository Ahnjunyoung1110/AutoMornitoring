package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Component
public class MonitoringWorker {

    private final ParseMediaManifest parseMediaManifest;
    private final GetMediaService getMediaService;
    private final RabbitTemplate rabbit;
    private final RedisMediaService redisMediaService;

    // 메시지를 받아 처리하는 함수, 처리에 실패하면 딜레이 큐로 보낸다.
    @RabbitListener(queues = RabbitNames.DELAY_STAGE1)
    void receiveMessage(CheckMediaManifestCmd cmd){
        Instant nowTime = Instant.now();

        // curl
        String media = getMediaService.getMedia(cmd.mediaUrl(), "");

        // 파싱
        RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(media);

        // redis에 저장
        addToRedis(cmd.traceId(),cmd.resolution(),recordMediaToRedisDTO);

        CheckMediaManifestCmd newCmd = new CheckMediaManifestCmd(cmd.mediaUrl(), cmd.resolution(),0, nowTime, cmd.traceId());

        Long delay = 5L - (Duration.between(cmd.publishTime(), nowTime).minusSeconds(5)).toMillis();

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
}