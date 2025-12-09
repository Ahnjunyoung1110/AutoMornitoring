package AutoMonitoring.AutoMonitoring.domain.program.exception;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Component("globalRabbitErrorHandler")
@Slf4j
public class ProgramRabbitErrorHandler implements RabbitListenerErrorHandler {

    @Override
    public Object handleError(Message message, Channel channel, org.springframework.messaging.Message<?> message1, ListenerExecutionFailedException e) throws Exception {
        Throwable cause = e.getCause() != null ? e.getCause() : e;

        if (cause instanceof ProgramNotFoundException pnfe) {
            log.warn("ProgramNotFoundException - 그냥 드랍하고 ACK 합니다. msg={}", message1, pnfe);
            // 여기서 예외를 다시 던지지 않으면 → ACK 후 드랍
            return null;
        }

        // 그 외 예외는 기존 정책(재시도/ DLX 등)에 태운다
        throw e;
    }
}
