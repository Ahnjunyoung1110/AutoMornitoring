package AutoMonitoring.AutoMonitoring.domain.api.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class recordManifestTest extends BaseTest {
    @Autowired
    RecordManifest recordManifest;

    @Test
    void recordMasterManifest() {
        String traceId = recordManifest.recordMasterManifest("Hi", "");
        Assertions.assertThat(traceId).isNotNull();
    }
}