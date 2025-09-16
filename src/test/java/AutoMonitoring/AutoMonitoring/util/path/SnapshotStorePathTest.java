package AutoMonitoring.AutoMonitoring.util.path;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SnapshotStorePathTest {

    private final SnapshotStorePath snapshotStorePath;

    // 생성자 주입
    SnapshotStorePathTest(SnapshotStorePath snapshotStorePath) {
        this.snapshotStorePath = snapshotStorePath;
    }

    @MockitoBean
    private Object redisServiceMock;


    @Test
    void m3u8Base() {
        Assertions.assertThat(snapshotStorePath.m3u8Base()).isNotNull();
    }

    @Test
    void tsBase() {
    }
}