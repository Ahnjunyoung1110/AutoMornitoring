package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.mqworker;


import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.dto.CheckValidDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class ValidateWorker {


    @RabbitListener(queues = RabbitNames.Q_VALID)
    void receiveMessage(CheckValidDTO dto){

    }
}
