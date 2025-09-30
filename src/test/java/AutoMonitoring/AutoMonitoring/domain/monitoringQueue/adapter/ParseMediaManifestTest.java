package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.ParseMediaManifestImpl;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.SnapshotStore;
import AutoMonitoring.AutoMonitoring.util.path.SnapshotStorePath;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ParseMediaManifestTest {

    private ParseMediaManifest parseMediaManifest;

    @Mock
    RedisService redisService;

    @MockitoBean
    SnapshotStorePath snapshotStorePath;

    @BeforeEach
    void setUp() {
        // SnapshotStore는 현재 구현에서 사용되지 않으므로 mock으로 처리
        SnapshotStore snapshotStore = mock(SnapshotStore.class);
        parseMediaManifest = new ParseMediaManifestImpl(snapshotStore, snapshotStorePath, redisService);
    }

    @Test
    @DisplayName("표준 HLS 매니페스트를 올바르게 파싱한다.")
    void parse_StandardManifest_ShouldParseCorrectly() {
        // given
        String standardManifest = """
                #EXTM3U
                #EXT-X-VERSION:6
                #EXT-X-MEDIA-SEQUENCE:10462006
                #EXT-X-TARGETDURATION:5
                #EXT-X-DISCONTINUITY-SEQUENCE:207886
                #EXTINF:5.0,
                https://test.com/segment1.ts
                #EXTINF:5.0,
                https://test.com/segment2.ts
                #EXTINF:5.0,
                https://test.com/segment3.ts
                """;

        // when
        RecordMediaToRedisDTO result = parseMediaManifest.parse(standardManifest, Duration.ZERO, "trace-1", "1080p");

        // then
        assertThat(result.seq()).isEqualTo(10462006);
        assertThat(result.dseq()).isEqualTo(207886);
        assertThat(result.segFirstUri()).isEqualTo("https://test.com/segment1.ts");
        assertThat(result.segLastUri()).isEqualTo("https://test.com/segment3.ts");
        assertThat(result.segmentCount()).isEqualTo(3);
        assertThat(result.disCount()).isZero();
        assertThat(result.wrongExtinf()).isFalse();
    }

    @Test
    @DisplayName("Discontinuity 태그가 포함된 매니페스트를 파싱하고 카운트를 올린다.")
    void parse_ManifestWithDiscontinuity_ShouldIncrementDiscontinuityCount() {
        // given
        String manifestWithDiscontinuity = """
                #EXTM3U
                #EXT-X-MEDIA-SEQUENCE:1
                #EXT-X-TARGETDURATION:5
                #EXTINF:5.0,
                https://test.com/segment1.ts
                #EXT-X-DISCONTINUITY
                #EXTINF:5.0,
                https://test.com/segment2.ts
                #EXT-X-DISCONTINUITY
                #EXTINF:5.0,
                https://test.com/segment3.ts
                """;

        when(snapshotStorePath.m3u8Base()).thenReturn(Path.of("1234"));

        // when
        RecordMediaToRedisDTO result = parseMediaManifest.parse(manifestWithDiscontinuity, Duration.ZERO, "trace-2", "720p");

        // then
        assertThat(result.disCount()).isEqualTo(2);
        assertThat(result.segmentCount()).isEqualTo(3);
        assertThat(result.wrongExtinf()).isFalse();
    }

    @Test
    @DisplayName("EXTINF 값이 Target Duration과 다른 경우 wrongExtinf 플래그를 true로 설정한다.")
    void parse_ManifestWithWrongExtinf_ShouldSetWrongExtinfFlag() {
        // given
        String manifestWithWrongExtinf = """
                #EXTM3U
                #EXT-X-TARGETDURATION:5
                #EXT-X-MEDIA-SEQUENCE:1
                #EXTINF:5.0,
                https://test.com/segment1.ts
                #EXTINF:3.1,
                https://test.com/segment2.ts
                #EXTINF:5.0,
                https://test.com/segment3.ts
                """;

        // when
        RecordMediaToRedisDTO result = parseMediaManifest.parse(manifestWithWrongExtinf, Duration.ZERO, "trace-3", "480p");

        // then
        assertThat(result.wrongExtinf()).isTrue();
        assertThat(result.disCount()).isZero();
        assertThat(result.segmentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("URI에 쿼리 파라미터가 있어도 올바르게 파싱한다.")
    void parse_ManifestWithQueryParameters_ShouldParseUriCorrectly() {
        // given
        String manifestWithQuery = """
                #EXTM3U
                #EXT-X-MEDIA-SEQUENCE:1
                #EXT-X-TARGETDURATION:5
                #EXTINF:5.0,
                https://test.com/segment1.ts?a=1&b=2
                """;

        // when
        RecordMediaToRedisDTO result = parseMediaManifest.parse(manifestWithQuery, Duration.ZERO, "trace-4", "1080p");

        // then
        assertThat(result.segFirstUri()).isEqualTo("https://test.com/segment1.ts");
        assertThat(result.segLastUri()).isEqualTo("https://test.com/segment1.ts");
        assertThat(result.segmentCount()).isEqualTo(1);
    }
}