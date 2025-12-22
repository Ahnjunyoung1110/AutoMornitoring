package AutoMonitoring.AutoMonitoring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitConfig {

    //<editor-fold desc="Basic Configs">
    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper om){
        var conv = new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter(om);
        conv.setCreateMessageIds(true);
        return conv;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate rt = new RabbitTemplate(cf);
        rt.setMessageConverter(mc);
        rt.setReplyTimeout(3000);
        return rt;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter mc
    ){
        var f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(mc);
        return f;
    }

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> probeContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory
    ){
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
    //</editor-fold>

    // 1. Exchange 정의
    @Bean DirectExchange provisioningExchange() { return new DirectExchange(RabbitNames.EX_PROVISIONING); }
    @Bean DirectExchange monitoringExchange() { return new DirectExchange(RabbitNames.EX_MONITORING); }
    @Bean DirectExchange delayExchange() { return new DirectExchange(RabbitNames.EX_DELAY); }
    @Bean DirectExchange monitoringCommandExchange(){ return new DirectExchange(RabbitNames.EX_MONITORING_COMMAND); }
    @Bean DirectExchange programCommandExchange(){ return new DirectExchange(RabbitNames.EX_PROGRAM_COMMAND); }
    @Bean DirectExchange checkValidCommandExchange(){ return new DirectExchange(RabbitNames.EX_CHECKVALID_COMMAND); }
    @Bean
    public DirectExchange validExchange() {
        return new DirectExchange(RabbitNames.EX_VALID);
    }
    @Bean TopicExchange deadLetterExchange() { return new TopicExchange(RabbitNames.EX_DEAD_LETTER); }


    // 2. Provisioning 토폴로지 (초기 등록)
    @Bean Queue qStage1() { return new Queue(RabbitNames.Q_STAGE1, true); }
    @Bean Queue qStage2() { return new Queue(RabbitNames.Q_STAGE2, true); }
    @Bean Queue qStage3() { return new Queue(RabbitNames.Q_STAGE3, true); }
    @Bean Queue monitoringCommandQueue() { return new Queue(RabbitNames.Q_MONITORING_COMMAND, true); }
    @Bean Queue programCommandQueue() { return new Queue(RabbitNames.Q_PROGRAM_COMMAND, true); }
    @Bean Queue checkValidCommandQueue() { return new Queue(RabbitNames.Q_CHECKVALID_COMMAND, true); }

    @Bean Binding bStage1() { return BindingBuilder.bind(qStage1()).to(provisioningExchange()).with(RabbitNames.RK_STAGE1); }
    @Bean Binding bStage2() { return BindingBuilder.bind(qStage2()).to(provisioningExchange()).with(RabbitNames.RK_STAGE2); }
    @Bean Binding bStage3() { return BindingBuilder.bind(qStage3()).to(provisioningExchange()).with(RabbitNames.RK_STAGE3); }
    @Bean Binding bMonitoringCommand() { return BindingBuilder.bind(monitoringCommandQueue()).to(monitoringCommandExchange()).with(RabbitNames.RK_MONITORING_COMMAND);}
    @Bean Binding bProgramCommand() { return BindingBuilder.bind(programCommandQueue()).to(programCommandExchange()).with(RabbitNames.RK_PROGRAM_COMMAND);}
    @Bean Binding bCheckValidCommand() { return BindingBuilder.bind(checkValidCommandQueue()).to(checkValidCommandExchange()).with(RabbitNames.RK_CHECKVALID_COMMAND);}


    @Bean
    public Declarables validTopology() {
        List<Declarable> declarables = new ArrayList<>();

        for (int i = 0; i < RabbitNames.VALID_PARTITIONS; i++) {
            String queueName = RabbitNames.qValid(i);      // "q.valid.0" ~
            String routingKey = RabbitNames.routingValid(i); // "valid.0" ~

            Queue q = QueueBuilder
                    .durable(queueName)
                    // 여기 TTL, DLX 등 기존 Q_VALID 옵션 그대로
                    // .ttl(...)
                    // .deadLetterExchange(...)
                    .build();

            Binding b = BindingBuilder
                    .bind(q)
                    .to(validExchange())
                    .with(routingKey);

            declarables.add(q);
            declarables.add(b);
        }

        return new Declarables(declarables);
    }

    // 3. Monitoring 토폴로지 (핵심 루프)
    @Bean Queue workQueue() {
        return QueueBuilder.durable(RabbitNames.Q_WORK)
                .deadLetterExchange(RabbitNames.EX_MONITORING)
                .deadLetterRoutingKey(RabbitNames.RK_WORK_DLX) // 실패 시 DLX로 보낼 라우팅 키
                .build();
    }
    @Bean Binding bWork() { return BindingBuilder.bind(workQueue()).to(monitoringExchange()).with(RabbitNames.RK_WORK); }


    // 4. Delay 토폴로지 (모니터링 주기용)
    private Queue createDelayQueue(String queueName) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(monitoringExchange().getName()) // 만료 후 monitoring exchange로
                .deadLetterRoutingKey(RabbitNames.RK_WORK) // work queue로 가도록
                .build();
    }
    @Bean Queue delayQueueDefault() { return createDelayQueue(RabbitNames.Q_DELAY_DEFAULT); }
    @Bean Queue delayQueue1s() { return createDelayQueue(RabbitNames.Q_DELAY_1S); }
    @Bean Queue delayQueue2s() { return createDelayQueue(RabbitNames.Q_DELAY_2S); }
    @Bean Queue delayQueue3s() { return createDelayQueue(RabbitNames.Q_DELAY_3S); }
    @Bean Queue delayQueue4s() { return createDelayQueue(RabbitNames.Q_DELAY_4S); }

    @Bean Binding bDelayDefault() { return BindingBuilder.bind(delayQueueDefault()).to(delayExchange()).with(RabbitNames.RK_DELAY_DEFAULT); }
    @Bean Binding bDelay1s() { return BindingBuilder.bind(delayQueue1s()).to(delayExchange()).with(RabbitNames.RK_DELAY_1S); }
    @Bean Binding bDelay2s() { return BindingBuilder.bind(delayQueue2s()).to(delayExchange()).with(RabbitNames.RK_DELAY_2S); }
    @Bean Binding bDelay3s() { return BindingBuilder.bind(delayQueue3s()).to(delayExchange()).with(RabbitNames.RK_DELAY_3S); }
    @Bean Binding bDelay4s() { return BindingBuilder.bind(delayQueue4s()).to(delayExchange()).with(RabbitNames.RK_DELAY_4S); }


    // 5. Retry 토폴로지 (실패 재시도용)
    @Bean Queue workDlxQueue() { // 실패한 메시지가 잠시 머무는 곳 (재시도 로직 수행)
        return QueueBuilder.durable(RabbitNames.Q_WORK_DLX)
                .deadLetterExchange(deadLetterExchange().getName())
                .deadLetterRoutingKey(RabbitNames.RK_DEAD) // 재시도도 최종 실패하면 DLX로
                .build();
    }
    @Bean Binding bWorkDlx() { return BindingBuilder.bind(workDlxQueue()).to(monitoringExchange()).with(RabbitNames.RK_WORK_DLX); }

    @Bean Queue retryDelayQueue1s() { // 1초 재시도 딜레이를 위한 큐
        return QueueBuilder.durable(RabbitNames.Q_RETRY_DELAY)
                .deadLetterExchange(monitoringExchange().getName()) // 만료 후 monitoring exchange로
                .deadLetterRoutingKey(RabbitNames.RK_WORK_DLX) // 재시도 큐(workDlxQueue)로 가도록
                .ttl(1000)
                .build();
    }
    @Bean Binding bRetryDelay() { return BindingBuilder.bind(retryDelayQueue1s()).to(delayExchange()).with(RabbitNames.RK_RETRY_DELAY); }


    // 6. Dead-Letter 토폴로지 (최종 실패)
    @Bean Queue deadLetterQueue()  { return QueueBuilder.durable(RabbitNames.Q_DEAD).build(); }

    @Bean Binding bDeadLetter() { // 모든 Dead Letter를 수집
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }
}
