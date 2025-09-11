package AutoMonitoring.AutoMonitoring;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.containers.wait.strategy.Wait;

public class TestRabbitMQContainer {

    private static final String USER = "myuser";
    private static final String PASS = "password";

    // 3.13 브로커용 delayed 플러그인(.ez)을 test/resources/plugins/ 에 넣어두세요.
    private static final ImageFromDockerfile IMAGE = new ImageFromDockerfile()
            .withFileFromString("Dockerfile", """
            FROM rabbitmq:3.13-management
            # test/resources/plugins/ 경로의 파일을 이미지로 복사
            ADD rabbitmq_delayed_message_exchange-3.13.0.ez /opt/rabbitmq/plugins/
            RUN rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange
            """)
            .withFileFromClasspath(
                    "rabbitmq_delayed_message_exchange-3.13.0.ez",
                    "plugins/rabbitmq_delayed_message_exchange-3.13.0.ez"
            );

    private static final GenericContainer<?> INSTANCE = new GenericContainer<>(IMAGE)
            .withExposedPorts(5672, 15672)
            .withEnv("RABBITMQ_DEFAULT_USER", USER)
            .withEnv("RABBITMQ_DEFAULT_PASS", PASS)
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1));

    static {
        INSTANCE.start(); // 클래스 로딩 시 1회 기동
    }

    // ---- 접근 헬퍼 ----
    public static String getHost() { return INSTANCE.getHost(); }
    public static Integer getAmqpPort() { return INSTANCE.getMappedPort(5672); }
    public static Integer getHttpPort() { return INSTANCE.getMappedPort(15672); }
    public static String getUsername() { return USER; }
    public static String getPassword() { return PASS; }

    // 필요 시 컨테이너 핸들 직접 쓰고 싶다면
    public static GenericContainer<?> getContainer() { return INSTANCE; }
}
