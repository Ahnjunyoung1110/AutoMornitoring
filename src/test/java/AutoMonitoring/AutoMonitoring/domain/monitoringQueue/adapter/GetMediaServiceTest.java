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
        String testUrl = "";

        String result = getMediaService.getMedia(testUrl, "");
        Assertions.assertThat(result).isNotNull();
    }
}