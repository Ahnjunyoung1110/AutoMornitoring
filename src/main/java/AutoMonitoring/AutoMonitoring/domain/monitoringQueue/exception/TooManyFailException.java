package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

public class TooManyFailException extends RuntimeException {
    public TooManyFailException(String message) {

        super(message);
        throw new AmqpRejectAndDontRequeueException("too many fail");

    }
}
