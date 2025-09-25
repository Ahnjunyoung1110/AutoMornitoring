//package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;
//
//import AutoMonitoring.AutoMonitoring.TestRabbitMQContainer;
//import AutoMonitoring.AutoMonitoring.TestRedisContainer;
//import AutoMonitoring.AutoMonitoring.config.RabbitNames;
//import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.MonitoringService;
//import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
//import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.amqp.core.*;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Map;
//
//import static java.time.Duration.between;
//
//
//@SpringBootTest
//@ActiveProfiles("test")
//@ExtendWith(TestRedisContainer.class)
//class MonitoringWorkerTest {
//
//    @Autowired
//    private MonitoringService monitoringService;
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//    @Autowired
//    private AmqpAdmin amqpAdmin;
//    @Autowired
//    private RedisService redis;
//
//    @Autowired
//    private MonitoringWorker monitoringWorker;
//
//
//    // rabbitMQ를 사용하기 위한 설정
//    @DynamicPropertySource
//    static void props(DynamicPropertyRegistry r) {
//        r.add("spring.rabbitmq.host", TestRabbitMQContainer::getHost);
//        r.add("spring.rabbitmq.port", TestRabbitMQContainer::getAmqpPort);
//        r.add("spring.rabbitmq.username", TestRabbitMQContainer::getUsername);
//        r.add("spring.rabbitmq.password", TestRabbitMQContainer::getPassword);
//    }
//
//    @BeforeEach
//    void resetTopology() {
//        // 싹 정리 후 재선언(매 테스트 독립성)
//        try {
//            amqpAdmin.deleteQueue(RabbitNames.D);
//        } catch (Exception ignored) {
//        }
//        try {
//            amqpAdmin.deleteQueue(RabbitNames.DELAY_STAGE2);
//        } catch (Exception ignored) {
//        }
//        try {
//            amqpAdmin.deleteExchange(RabbitNames.DRK_STAGE1);
//        } catch (Exception ignored) {
//        }
//        try {
//            amqpAdmin.deleteExchange(RabbitNames.DRK_STAGE2);
//        } catch (Exception ignored) {
//        }
//
//
//        // x-delayed-message 교환 선언
//        Map<String, Object> args = Map.of("x-delayed-type", "direct");
//        CustomExchange delayedEx = new CustomExchange(RabbitNames.DELAY_PIPELINE, "x-delayed-message", true, false, args);
//        amqpAdmin.declareExchange(delayedEx);
//
//        // 큐 & 바인딩
//        amqpAdmin.declareQueue(new Queue(RabbitNames.DELAY_STAGE1, true));
//        amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(RabbitNames.DELAY_STAGE1)).to(delayedEx).with(RabbitNames.DRK_STAGE1).noargs());
//
//        amqpAdmin.declareQueue(new Queue(RabbitNames.DELAY_STAGE2, true));
//        amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(RabbitNames.DELAY_STAGE2)).to(delayedEx).with(RabbitNames.DRK_STAGE2).noargs());
//
//    }
//
//
//    // 정상적으로 수신하는경우 5초 뒤 queue에 입력
//    @Test
//    void receiveMessage() {
//        Instant now = Instant.now();
//        String testUrl = "https://ssai.aniview.com/api/v1/hls/streams/sessions/2ff6df3b46284cf5ac00329e1f313866/media/index.m3u8/1.m3u8";
//        CheckMediaManifestCmd cmd = new CheckMediaManifestCmd(testUrl,"1080","1234",
//                0, now, "testTraceId");
//        monitoringWorker.receiveMessage(cmd);
//
//        CheckMediaManifestCmd receiveCmd1 =
//                (CheckMediaManifestCmd) rabbitTemplate.receiveAndConvert(RabbitNames.DELAY_STAGE1, 20_000);
//
//
//        Assertions.assertThat(receiveCmd1).isNotNull();
//        Assertions.assertThat(receiveCmd1.traceId()).isEqualTo("testTraceId");
//        Assertions.assertThat(receiveCmd1.failCount()).isEqualTo(0);
//        Assertions.assertThat(between(receiveCmd1.publishTime(),now.plusSeconds(5))).isLessThan(Duration.ofSeconds(2));
//        Assertions.assertThat(receiveCmd1.mediaUrl()).isEqualTo(testUrl);
//
//    }
//
//    @Test
//        // url의 수신에 실패한경우
//    void receiveMessageFail() {
//        Instant now = Instant.now();
//        String testUrl = "TestFailUrl";
//        CheckMediaManifestCmd cmd = new CheckMediaManifestCmd(testUrl,"1080","1234",
//                0, now, "testTraceId");
//        monitoringWorker.receiveMessage(cmd);
//
//        Message received = (Message) rabbitTemplate.receiveAndConvert(RabbitNames.DELAY_STAGE2, 2000);
//        Assertions.assertThat(received).isNotNull();
//
//        // 바디는 컨버터로 DTO 변환
//        CheckMediaManifestCmd receiveCmd1 =
//                (CheckMediaManifestCmd) rabbitTemplate.getMessageConverter().fromMessage(received);
//        Assertions.assertThat(receiveCmd1.traceId()).isEqualTo("testTraceId");
//        Assertions.assertThat(receiveCmd1.failCount()).isEqualTo(1);
//        // 2초 뒤에 재시도
//        Assertions.assertThat(receiveCmd1.publishTime()).isEqualTo(now.plusSeconds(2));
//        Assertions.assertThat(receiveCmd1.mediaUrl()).isEqualTo(testUrl);
//    }
//}