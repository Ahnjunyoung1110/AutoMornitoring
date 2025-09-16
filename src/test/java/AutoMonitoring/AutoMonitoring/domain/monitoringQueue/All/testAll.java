package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.All;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.GetMediaServiceImpl;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.ParseMediaManifestImpl;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.SnapshotStore;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest
@RequiredArgsConstructor
public class testAll {

    GetMediaService getMediaService = new GetMediaServiceImpl();
    private final ParseMediaManifest parseMediaManifest;

    @Test
    void curlAndParse(){
        String testUrl = "https://ssai.aniview.com/api/v1/hls/streams/sessions/6cdc6db0e9424e7f8f6726616a998468/media/index.m3u8/0.m3u8";

        String result = getMediaService.getMedia(testUrl, "");
        RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(result, Duration.ZERO, "123", "1080");
        System.out.println(recordMediaToRedisDTO);

    }
}
