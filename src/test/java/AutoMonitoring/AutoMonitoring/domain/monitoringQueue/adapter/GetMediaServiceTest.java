package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.GetMediaServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GetMediaServiceTest extends BaseTest {

    @Autowired
    GetMediaService getMediaService;


    @Test
    void getMedia() {
        String testUrl = "";

        String result = getMediaService.getMedia(testUrl, "");
        Assertions.assertThat(result).isNotNull();
    }
}