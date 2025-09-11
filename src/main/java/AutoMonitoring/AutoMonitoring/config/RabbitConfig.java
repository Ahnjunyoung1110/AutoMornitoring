package AutoMonitoring.AutoMonitoring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
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

    // 토폴로지
    @Bean DirectExchange ex() { return new DirectExchange(RabbitNames.EX_PIPELINE); }
    @Bean Queue q2() { return new Queue(RabbitNames.Q_STAGE1, true); }
    @Bean Queue q3() { return new Queue(RabbitNames.Q_STAGE2, true); }
    @Bean Binding b2() { return BindingBuilder.bind(q2()).to(ex()).with(RabbitNames.RK_STAGE1); }
    @Bean Binding b3() { return BindingBuilder.bind(q3()).to(ex()).with(RabbitNames.RK_STAGE2); }


    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("x-delayed-type", "direct");   // or "topic", "fanout" 등

        return new CustomExchange(
                RabbitNames.DELAY_PIPELINE,         // ex. "ex.delay.pipeline"
                "x-delayed-message",
                true,   // durable
                false,  // autoDelete
                args
        );
    }

    @Bean Queue fq1() { return new Queue(RabbitNames.DELAY_STAGE1, true); }
    @Bean Queue fq2() { return new Queue(RabbitNames.DELAY_STAGE2, true); }
    @Bean Binding fb1() { return BindingBuilder.bind(fq1()).to(delayExchange()).with(RabbitNames.DRK_STAGE1).noargs(); }
    @Bean Binding fb2() { return BindingBuilder.bind(fq2()).to(delayExchange()).with(RabbitNames.DRK_STAGE2).noargs(); }
}
