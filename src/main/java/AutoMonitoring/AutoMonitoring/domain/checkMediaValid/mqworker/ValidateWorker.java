package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.mqworker;


import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter.ValidateCheckService;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application.AlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class ValidateWorker {

    private final ValidateCheckService validateCheckService;
    private final AlarmService alarmService;

    @RabbitListener(queues = RabbitNames.Q_VALID)
    void receiveMessage(CheckValidDTO dto){
        log.info("위아 쩨낑 on {}, {}", dto.traceId(), dto.resolution());
        ValidationResult validationResult = validateCheckService.checkValidation(dto);
        log.info("위 껌쁠리뜨 쩨낑 {}, {}    result: {}", dto.traceId(), dto.resolution(), validationResult);

        if(!validationResult.equals(ValidationResult.OK_FINE)){
            // 알람!
            alarmService.PublishAlarm(validationResult.toString(), dto.resolution(), dto.traceId());
            log.warn("이상현상 발생 {}", validationResult.toString());
        }
    }
}
