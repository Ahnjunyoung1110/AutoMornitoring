package AutoMonitoring.AutoMonitoring.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.rabbitmq.*;

@Configuration
public class RabbitReactiveConfig {

    @Bean
    public Receiver reactiveReceiver(org.springframework.amqp.rabbit.connection.ConnectionFactory springCf){
        com.rabbitmq.client.ConnectionFactory javaCf =
                ((org.springframework.amqp.rabbit.connection.CachingConnectionFactory) springCf)
                        .getRabbitConnectionFactory();

        ReceiverOptions opts = new ReceiverOptions().connectionFactory(javaCf);

        return RabbitFlux.createReceiver(opts);
    }

    @Bean
    public Sender reactiveSender(org.springframework.amqp.rabbit.connection.ConnectionFactory springCf) {

        com.rabbitmq.client.ConnectionFactory javaCf =
                ((org.springframework.amqp.rabbit.connection.CachingConnectionFactory) springCf)
                        .getRabbitConnectionFactory();

        SenderOptions opts = new SenderOptions().connectionFactory(javaCf);

        return RabbitFlux.createSender(opts);
    }

}
