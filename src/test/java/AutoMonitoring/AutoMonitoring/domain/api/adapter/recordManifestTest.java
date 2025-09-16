package AutoMonitoring.AutoMonitoring.domain.api.adapter;

import AutoMonitoring.AutoMonitoring.TestRedisContainer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestRedisContainer.class)

class recordManifestTest {

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Autowired
    RecordManifest recordManifest;

    @Test
    void recordMasterManifest() {
        String traceId = recordManifest.recordMasterManifest("Hi", "");
        Assertions.assertThat(traceId).isNotNull();
    }
}