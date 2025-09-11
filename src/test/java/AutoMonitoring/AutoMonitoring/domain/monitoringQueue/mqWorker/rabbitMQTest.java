package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;


import AutoMonitoring.AutoMonitoring.TestRabbitMQContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class rabbitMQTest {

    // rabbitMQ를 사용하기 위한 설정
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.rabbitmq.host", TestRabbitMQContainer::getHost);
        r.add("spring.rabbitmq.port", TestRabbitMQContainer::getAmqpPort);
        r.add("spring.rabbitmq.username", TestRabbitMQContainer::getUsername);
        r.add("spring.rabbitmq.password", TestRabbitMQContainer::getPassword);

    }

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    AmqpAdmin amqpAdmin;

    final String DEX = "delayex.test";

    final String FRK = "failrk.test";
    final String FQ = "failq.test";

    @BeforeEach
    void declare() {
        // 지연 교환: 타입/아규먼트 일치해야 함 (기존 타입과 다르면 PRECONDITION_FAILED 남)
        var args = new java.util.HashMap<String, Object>();
        args.put("x-delayed-type", "direct");
        var delayedEx = new org.springframework.amqp.core.CustomExchange(
                DEX, "x-delayed-message", true, false, args);

        amqpAdmin.declareExchange(delayedEx);
        amqpAdmin.declareQueue(new Queue(FQ, true));
        amqpAdmin.declareBinding(
                BindingBuilder.bind(new Queue(FQ)).to(delayedEx).with(FRK).noargs());
    }


    // delay가 되는지 확인
    @Test
    void delayed_publish_and_consume() {
        rabbitTemplate.convertAndSend(DEX, FRK, "hello delayed", m -> {
            m.getMessageProperties().setHeader("x-delay", 500);
            return m;
        });

        // 딜레이가 되었다면 Null이어야함
        var immediate = rabbitTemplate.receiveAndConvert(FQ, 100);
        assertThat(immediate).isNull();

        // 일정 시간 기다렸기 때문에 메시지를 수신해야함
        var received = rabbitTemplate.receiveAndConvert(FQ, 2000);
        assertThat(received).isEqualTo("hello delayed");
    }

    // 정상 queue 에서 수신된 경우

    // 비정상 queue 에서 수신된 경우


}
