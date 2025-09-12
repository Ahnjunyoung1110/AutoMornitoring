package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.TestRabbitMQContainer;
import AutoMonitoring.AutoMonitoring.TestRedisContainer;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestRedisContainer.class)
class MonitoringServiceTest {

    @Autowired
    private MonitoringService monitoringService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RedisService redis;

    final String Q = RabbitNames.DELAY_STAGE1;
    final String DEX = RabbitNames.DELAY_PIPELINE;
    final String RK = RabbitNames.DRK_STAGE1;

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry r) {
        // RabbitMQ
        r.add("spring.rabbitmq.host", TestRabbitMQContainer::getHost);
        r.add("spring.rabbitmq.port", TestRabbitMQContainer::getAmqpPort);
        r.add("spring.rabbitmq.username", TestRabbitMQContainer::getUsername);
        r.add("spring.rabbitmq.password", TestRabbitMQContainer::getPassword);

    }


    @BeforeEach
    void resetTopology() {
        // 싹 정리 후 재선언(매 테스트 독립성)
        try { amqpAdmin.deleteQueue(Q); } catch (Exception ignored) {}
        try { amqpAdmin.deleteExchange(DEX); } catch (Exception ignored) {}

        // x-delayed-message 교환 선언
        Map<String,Object> args = Map.of("x-delayed-type", "direct");
        CustomExchange delayedEx = new CustomExchange(DEX, "x-delayed-message", true, false, args);
        amqpAdmin.declareExchange(delayedEx);

        // 큐 & 바인딩
        amqpAdmin.declareQueue(new Queue(Q, true));
        amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(Q)).to(delayedEx).with(RK).noargs());
    }

    @Test
    void startMornitoring() {
        StartMonitoringDTO startMonitoringDTO = new StartMonitoringDTO("qwer", "https://ssai.aniview.com/api/v1/hls/streams/sessions/172f31b1184a4d36bde90a6b9b264fef/media/index.m3u8/1.m3u8","1920x1080");
        monitoringService.startMornitoring(startMonitoringDTO);

        Message msg = rabbitTemplate.receive(Q, 2000);
        if (msg == null) {
            Assertions.assertThat("wrong").isEqualTo("wrongggg");
        }
        Object raw = msg.getMessageProperties().getHeaders().get("x-delay");
        Long xDelay = raw == null ? null : Long.valueOf(raw.toString()); // Integer/Long 가능성
        // 바디는 컨버터로 DTO 변환
        CheckMediaManifestCmd cmd =
                (CheckMediaManifestCmd) rabbitTemplate.getMessageConverter().fromMessage(msg);

        Assertions.assertThat(cmd.failCount()).isEqualTo(0);
        Assertions.assertThat(cmd.traceId()).isEqualTo("qwer");
        Assertions.assertThat(cmd.mediaUrl()).isEqualTo("https://ssai.aniview.com/api/v1/hls/streams/sessions/172f31b1184a4d36bde90a6b9b264fef/media/index.m3u8/1.m3u8");


        // redis에서의 상태가 Ok
        Assertions.assertThat(redis.getValues("https://ssai.aniview.com/api/v1/hls/streams/sessions/172f31b1184a4d36bde90a6b9b264fef/media/index.m3u8/1.m3u8"))
                .isEqualTo("MONITORING");
        // traceId는 MONITORING
        Assertions.assertThat(redis.getValues(cmd.traceId())).isEqualTo("MONITORING");


    }

    @Test
    void startMornitoringWithWrongUrl() {
        StartMonitoringDTO startMonitoringDTO = new StartMonitoringDTO("qwer", "wrong", "1020");
        Assertions.assertThatRuntimeException().isThrownBy(() -> monitoringService.startMornitoring(startMonitoringDTO));


        // redis 에서의 traceId의 status가 Wrong
        Assertions.assertThat(redis.getValues("qwer")).isEqualTo("WRONG");
        // 이후 message queue가 null이어야 한다.
        Message msg = rabbitTemplate.receive(RabbitNames.DELAY_STAGE1, 2000);
        Assertions.assertThat(msg).isNull();

    }

    // 아래로는 추후 구현
//
//    @Test
//    void stopMornitoring() {
//        StartMonitoringDTO startMonitoringDTO = new StartMonitoringDTO("qwer", "wrong");
//        Assertions.assertThatRuntimeException().isThrownBy(() -> monitoringService.startMornitoring(startMonitoringDTO));
//
//        //redis 에서 status 가 MONITORING
//        StopMornitoringDTO stopMornitoringDTO = new StopMornitoringDTO("qwer", "어쩌구");
//        Assertions.assertThat(redis.getValues("어쩌구")).isEqualTo("MONITORING");
//
//        // 중지
//
//
//        //redis 에서 status 가 stopped
//
//
//
//
//    }
//
//    @Test
//    void stopMornitoringX() {
//
//        // 그런 데이터 redis 에 없더라 야 라는 에러
//
//    }

}