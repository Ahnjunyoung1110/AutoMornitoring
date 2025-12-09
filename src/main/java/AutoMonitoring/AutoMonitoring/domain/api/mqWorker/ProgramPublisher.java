package AutoMonitoring.AutoMonitoring.domain.api.mqWorker;


import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.Command;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProgramPublisher {

    private final RabbitTemplate rabbit;

    public void publish(Command command){

        rabbit.convertAndSend(
                RabbitNames.EX_PROGRAM_COMMAND,
                RabbitNames.RK_PROGRAM_COMMAND,
                command);
    }

    public <R> R pulishAndGetReturn(Command command, ParameterizedTypeReference<R> type){
        return rabbit.convertSendAndReceiveAsType(
                RabbitNames.EX_PROGRAM_COMMAND,
                RabbitNames.RK_PROGRAM_COMMAND,
                command,
                type);

    }

}
