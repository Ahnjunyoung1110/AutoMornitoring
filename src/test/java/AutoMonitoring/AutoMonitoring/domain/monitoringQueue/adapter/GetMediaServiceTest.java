package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.GetMediaServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class GetMediaServiceTest {

    GetMediaService getMediaService = new GetMediaServiceImpl();


    @Test
    void getMedia() {
        String testUrl = "https://ssai.aniview.com/api/v1/hls/streams/sessions/6cdc6db0e9424e7f8f6726616a998468/media/index.m3u8/0.m3u8";

        String result = getMediaService.getMedia(testUrl, "");
        Assertions.assertThat(result).isNotNull();
    }
}