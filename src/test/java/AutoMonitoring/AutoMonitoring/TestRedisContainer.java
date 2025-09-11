package AutoMonitoring.AutoMonitoring;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * JUnit5 + Testcontainers에서 재사용 가능한 Redis 컨테이너 싱글톤.
 * - 이미지: redis:7-alpine
 * - 패스워드: redispw
 * - 포트: 6379 (동적으로 매핑)
 *
 * 사용법 (Spring Boot 테스트):
 *
 * @SpringBootTest
 * class SomeTest {
 *   @DynamicPropertySource
 *   static void redisProps(DynamicPropertyRegistry r) {
 *     r.add("spring.data.redis.host", TestRedisContainer::getHost);
 *     r.add("spring.data.redis.port", TestRedisContainer::getMappedPort);
 *     r.add("spring.data.redis.password", TestRedisContainer::getPassword);
 *   }
 * }
 */
public final class TestRedisContainer implements BeforeAllCallback {

    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:7-alpine");
    private GenericContainer redisContainer;

    // 필요한 경우 .withReuse(true)로 테스트 간 재사용 가능 (testcontainers.properties 필요)

    @Override
    public void beforeAll(ExtensionContext context){
        redisContainer = new GenericContainer<>(REDIS_IMAGE)
                .withCommand("redis-server", "--appendonly", "yes")
                .withExposedPorts(6379);
        redisContainer.start();

        System.setProperty("spring.data.redis.host", redisContainer.getHost());
        System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(6379).toString());

    }

    /** 테스트용 비밀번호 */
    public static String getPassword() {
        return "redispw";
    }
}
