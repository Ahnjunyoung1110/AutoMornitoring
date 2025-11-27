package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.mqworker;


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

    // 공통 처리 로직
    private void handle(CheckValidDTO dto) {
        log.info("위아 쩨낑 on {}, {}", dto.traceId(), dto.resolution());
        ValidationResult validationResult = validateCheckService.checkValidation(dto);
        log.info("위 껌쁠리뜨 쩨낑 {}, {}    result: {}",
                dto.traceId(), dto.resolution(), validationResult);

        // OK가 아니면 일단 로그는 남기고,
        if (validationResult != ValidationResult.OK_FINE) {
            log.warn("이상현상 발생 {}", validationResult);
        }

        // ERROR로 시작하는 경우에만 알람 발송
        if (validationResult.name().startsWith("ERROR_")) {
            alarmService.publishAlarm(validationResult.toString(), dto.resolution(), dto.traceId());
        }
    }

    @RabbitListener(queues = "q.valid.0", concurrency = "1")
    void receive0(CheckValidDTO dto){ handle(dto); }

    @RabbitListener(queues = "q.valid.1", concurrency = "1")
    void receive1(CheckValidDTO dto){ handle(dto); }

    @RabbitListener(queues = "q.valid.2", concurrency = "1")
    void receive2(CheckValidDTO dto){ handle(dto); }

    @RabbitListener(queues = "q.valid.3", concurrency = "1")
    void receive3(CheckValidDTO dto){ handle(dto); }

    @RabbitListener(queues = "q.valid.4", concurrency = "1")
    void receive4(CheckValidDTO dto){ handle(dto); }

    @RabbitListener(queues = "q.valid.5", concurrency = "1")
    void receive5(CheckValidDTO dto){ handle(dto); }

    @RabbitListener(queues = "q.valid.6", concurrency = "1")
    void receive6(CheckValidDTO dto){ handle(dto); }

    @RabbitListener(queues = "q.valid.7", concurrency = "1")
    void receive7(CheckValidDTO dto){ handle(dto); }


}
