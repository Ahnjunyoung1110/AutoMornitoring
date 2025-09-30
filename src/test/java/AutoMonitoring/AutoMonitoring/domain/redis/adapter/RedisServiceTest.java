package AutoMonitoring.AutoMonitoring.domain.redis.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RedisServiceTest extends BaseTest {

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("키-값 쌍을 저장하고 조회할 수 있다.")
    void setAndGetValues_Single() {
        // given
        String key = "testKey";
        String value = "testValue";

        // when
        redisService.setValues(key, value);
        String retrievedValue = redisService.getValues(key);

        // then
        assertThat(retrievedValue).isEqualTo(value);
    }

    @Test
    @DisplayName("여러 개의 키-값 쌍을 한 번에 조회할 수 있다.")
    void getValues_Multiple() {
        // given
        redisService.setValues("key1", "value1");
        redisService.setValues("key2", "value2");

        // when
        List<String> values = redisService.getValues(List.of("key1", "key2", "nonexistentKey"));

        // then
        assertThat(values).hasSize(3);
        assertThat(values).containsExactly("value1", "value2", null);
    }

    @Test
    @DisplayName("패턴에 맞는 키 목록을 조회할 수 있다.")
    void getKeys() {
        // given
        redisService.setValues("test:1", "a");
        redisService.setValues("test:2", "b");
        redisService.setValues("other:1", "c");

        // when
        Set<String> keys = redisService.getKeys("test:*");

        // then
        assertThat(keys).hasSize(2).containsExactlyInAnyOrder("test:1", "test:2");
    }

    @Test
    @DisplayName("키를 삭제할 수 있다.")
    void deleteValues() {
        // given
        String key = "deleteKey";
        redisService.setValues(key, "value");
        assertThat(redisService.getValues(key)).isNotNull();

        // when
        redisService.deleteValues(key);

        // then
        String valueAfterDeletion = redisService.getValues(key);
        assertThat(valueAfterDeletion).isEqualTo("false");
    }
}