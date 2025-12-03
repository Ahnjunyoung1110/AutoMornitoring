package AutoMonitoring.AutoMonitoring.domain.api.mqWorker;


import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.program.ProgramCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProgramPublisher {

    private final RabbitTemplate rabbit;

    public void publish(ProgramCommand command){

        rabbit.convertAndSend(
                RabbitNames.EX_PROGRAM_COMMAND,
                RabbitNames.RK_PROGRAM_COMMAND,
                command);
    }

}
