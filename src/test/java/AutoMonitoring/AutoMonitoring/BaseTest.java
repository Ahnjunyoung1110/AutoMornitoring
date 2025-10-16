package AutoMonitoring.AutoMonitoring;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;


@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
@ActiveProfiles("test")
@Slf4j
public abstract class BaseTest {

    @ServiceConnection
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .waitingFor(Wait.forListeningPort())
                    .withReuse(true)
                    .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    public RabbitListenerEndpointRegistry registry;

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


    @BeforeEach
    void setUp() {
        // 모든 리스너 컨테이너 중지
        registry.getListenerContainers().forEach(container -> {
            if (container.isRunning()) {
                container.stop();
            }
        });
    }
}
