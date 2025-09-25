package AutoMonitoring.AutoMonitoring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    // 메시지 직렬화 함수 가져오기
    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper om){
        var conv = new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
        conv.setCreateMessageIds(true);
        return conv;
    }

    //RabbitTemplate 에 컨버터 주입
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate rt = new RabbitTemplate(cf);
        rt.setMessageConverter(mc);
        rt.setReplyTimeout(3000);
        return rt;
    }

    // 소비자도 등록
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter mc
    ){
        var f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(mc);
        return f;
    }

    // 팩토리
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> probeContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory
    ){
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setPrefetchCount(10); // prefetch 값을 설정하는 부분

        return factory;

    }

    // 토폴로지
    @Bean DirectExchange ex() { return new DirectExchange(RabbitNames.EX_PIPELINE); }
    @Bean Queue q1() { return new Queue(RabbitNames.Q_STAGE1, true); }
    @Bean Queue q2() { return new Queue(RabbitNames.Q_STAGE2, true); }
    @Bean Queue q3() {return new Queue(RabbitNames.Q_STAGE3, true); }

    @Bean Binding b1() { return BindingBuilder.bind(q1()).to(ex()).with(RabbitNames.RK_STAGE1); }
    @Bean Binding b2() { return BindingBuilder.bind(q2()).to(ex()).with(RabbitNames.RK_STAGE2); }
    @Bean Binding b3() { return BindingBuilder.bind(q3()).to(ex()).with(RabbitNames.RK_STAGE3); }

    @Bean Queue valid_q(){ return new Queue(RabbitNames.Q_VALID, true); }
    @Bean Binding valid_b() { return BindingBuilder.bind(valid_q()).to(ex()).with(RabbitNames.RK_VALID);}

    @Bean Queue delayQ(){
        return QueueBuilder.durable(RabbitNames.ONLY_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitNames.EX_PIPELINE)
                .withArgument("x-dead-letter-routing-key", RabbitNames.WORK_STAGE1)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }

    @Bean Queue delayQ1S(){
        return QueueBuilder.durable(RabbitNames.ONLY_DELAY_QUEUE_1S)
                .withArgument("x-dead-letter-exchange", RabbitNames.EX_PIPELINE)
                .withArgument("x-dead-letter-routing-key", RabbitNames.WORK_STAGE1)
                .build();
    }

    @Bean Queue delayQ2S(){
        return QueueBuilder.durable(RabbitNames.ONLY_DELAY_QUEUE_2S)
                .withArgument("x-dead-letter-exchange", RabbitNames.EX_PIPELINE)
                .withArgument("x-dead-letter-routing-key", RabbitNames.WORK_STAGE1)
                .build();
    }

    @Bean Queue delayQ3S(){
        return QueueBuilder.durable(RabbitNames.ONLY_DELAY_QUEUE_3S)
                .withArgument("x-dead-letter-exchange", RabbitNames.EX_PIPELINE)
                .withArgument("x-dead-letter-routing-key", RabbitNames.WORK_STAGE1)
                .build();
    }

    @Bean Queue delayQ4S(){
        return QueueBuilder.durable(RabbitNames.ONLY_DELAY_QUEUE_4S)
                .withArgument("x-dead-letter-exchange", RabbitNames.EX_PIPELINE)
                .withArgument("x-dead-letter-routing-key", RabbitNames.WORK_STAGE1)
                .build();
    }

    @Bean Queue fq1() { return new Queue(RabbitNames.WORK_QUEUE, true); }
    @Bean Queue fq2() { return new Queue(RabbitNames.WORK_DLX_QUEUE, true); }
    @Bean Binding fb1() { return BindingBuilder.bind(fq1()).to(ex()).with(RabbitNames.WORK_STAGE1); }
    @Bean Binding fb2() { return BindingBuilder.bind(fq2()).to(ex()).with(RabbitNames.WORK_STAGE2); }
}
