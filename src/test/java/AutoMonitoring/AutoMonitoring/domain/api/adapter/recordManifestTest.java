package AutoMonitoring.AutoMonitoring.domain.api.adapter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class recordManifestTest {

    @Autowired RecordManifest recordManifest;
    @Test
    void recordMasterManifest() {
        String traceId = recordManifest.recordMasterManifest("Hi");
        Assertions.assertThat(traceId).isNotNull();
    }
}