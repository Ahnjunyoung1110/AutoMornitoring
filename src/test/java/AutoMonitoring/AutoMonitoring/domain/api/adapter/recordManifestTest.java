package AutoMonitoring.AutoMonitoring.domain.api.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.URLTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RecordManifestTest extends BaseTest {

    @Autowired
    private RecordManifest recordManifest;

    @Test
    @DisplayName("마스터 매니페스트 정보를 기록하면 traceId를 반환한다.")
    void recordMasterManifest_ShouldReturnTraceId() {
        // given
        String url = URLTestConfig.SUCCESS_MANIFEST_URL;
        String userAgent = "TestAgent";

        // when
        String traceId = recordManifest.recordMasterManifest(url, userAgent);

        // then
        assertThat(traceId).isNotNull().isNotBlank();
    }
}
