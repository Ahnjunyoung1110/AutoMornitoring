package AutoMonitoring.AutoMonitoring;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;


@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {

    @ServiceConnection
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .waitingFor(Wait.forListeningPort())
                    .withReuse(true)
                    .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redis;

    // rabbitMQ설정
    private static final String USER = "myuser";
    private static final String PASS = "secret";

    @ServiceConnection
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.13-management")
                    .withUser(USER, PASS)
                    .withVhost("vh_test")
                    .withPermission("vh_test", USER, ".*", ".*", ".*")
                    .withExposedPorts(5672, 15672)
                    .withReuse(true)
                    .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1))
                    .withStartupTimeout(Duration.ofSeconds(60));


    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.rabbitmq.virtual-host", () -> "vh_test");
    }

    @BeforeEach
    void resetState() throws Exception {
        // Redis, Rabbit 전체 초기화
        var r = REDIS.execInContainer("redis-cli", "FLUSHALL");
        RABBIT.execInContainer("bash","-lc",
                "for q in $(rabbitmqadmin --vhost=vh_test --format=tsv list queues name | tail -n +2); do " +
                        "  rabbitmqadmin --vhost=vh_test purge queue name=$q; " +
                        "done"
        );
    }
}
