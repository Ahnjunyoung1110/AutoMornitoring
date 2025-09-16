package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter;

import AutoMonitoring.AutoMonitoring.TestRedisContainer;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestRedisContainer.class)
class ValidateCheckServiceTest {
    @Autowired
    RedisMediaService redis;

    @Autowired
    ValidateCheckService validateCheckService;

    @Test
    void checkValidation() {
        RecordMediaToRedisDTO record1 = new RecordMediaToRedisDTO(Instant.now(), Duration.ofMillis(5L), 12345L, 123L, 0, 10, "",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0138.ts",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0147",
                "", false);
        redis.pushHistory("1234", "1080", record1, 10);

        RecordMediaToRedisDTO record2 = new RecordMediaToRedisDTO(Instant.now(), Duration.ofMillis(5L), 12346L, 123L, 0, 10, "",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0139.ts",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0148",
                "", false);

        redis.pushHistory("1234", "1080", record2, 10);

        Boolean check = validateCheckService.checkValidation("1234", "1080");
        Assertions.assertThat(check).isTrue();
    }

    @Test
    void notValidateSeq() {
        RecordMediaToRedisDTO record1 = new RecordMediaToRedisDTO(Instant.now(), Duration.ofMillis(5L), 12345L, 123L, 0, 10, "",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0138.ts",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0147",
                "", false);
        redis.pushHistory("1234", "1080", record1, 10);

        RecordMediaToRedisDTO record2 = new RecordMediaToRedisDTO(Instant.now(), Duration.ofMillis(5L), 12345L, 123L, 0, 10, "",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0139.ts",
                "https://cdn88.its-newid.net/asset/009f347cd24ff6e69b4338826aecc7befbfea130/hevc-1de83e86-aa1c88ce-f420f92e-e6700bd90-ts0148",
                "", false);
        redis.pushHistory("1234", "1080", record1, 10);

        Boolean check = validateCheckService.checkValidation("1234", "1080");
        Assertions.assertThat(check).isFalse();


    }
}