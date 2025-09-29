package AutoMonitoring.AutoMonitoring;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {
    @Container
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withReuse(true)
                    .waitingFor(Wait.forListeningPort())
                    .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redis;

    // rabbitMQ설정
    private static final String USER = "myuser";
    private static final String PASS = "secret";

    @Container
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.13-management")
                    .withUser(USER,PASS)
                    .withEnv("RABBITMQ_DEFAULT_VHOST", "vh_test")
                    .withReuse(true);


    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        r.add("spring.rabbitmq.host", RABBIT::getHost);
        r.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        r.add("spring.rabbitmq.username", () -> USER);
        r.add("spring.rabbitmq.password", () -> PASS);
        r.add("spring.rabbitmq.virtual-host", () -> "vh_test");
    }

    @BeforeAll
    static void ensurePerms() throws Exception {
        RABBIT.execInContainer("rabbitmqctl", "add_vhost", "vh_test");
        RABBIT.execInContainer("rabbitmqctl", "add_user", USER, PASS);
        RABBIT.execInContainer("rabbitmqctl", "set_permissions", "-p", "vh_test",
                USER, ".*", ".*", ".*");
    }
}
