package AutoMonitoring.AutoMonitoring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @Bean DirectExchange ex() { return new DirectExchange("ex.pipeline"); }
    @Bean Queue q2() { return new Queue("queue.stage2", true); }
    @Bean Queue q3() { return new Queue("queue.stage3", true); }

    @Bean Binding b2() { return BindingBuilder.bind(q2()).to(ex()).with("route.stage2"); }
    @Bean Binding b3() { return BindingBuilder.bind(q3()).to(ex()).with("route.stage3"); }
}
