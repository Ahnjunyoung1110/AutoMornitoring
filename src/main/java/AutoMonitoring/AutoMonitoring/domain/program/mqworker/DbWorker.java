package AutoMonitoring.AutoMonitoring.domain.program.mqworker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.program.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.program.dto.DbCommand;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbWorker {
    private final ProgramService programService;
    private final RedisService redis;
    private final RabbitTemplate rabbit;

    // 일딴은 insert만 구현
    @RabbitListener(queues = RabbitNames.Q_STAGE2)
    public void handle(DbCommand cmd){
        // db 에 저장
        Program program = Program.fromDto(cmd.probeDTO());
        Program savedProgram = programService.saveProgram(program);

        redis.setValues(cmd.traceId(), "MONITORING");

        // 이후 모니터링을 시행
        ProgramInfo stream = ProgramInfo.getProgramInfo(savedProgram);
        rabbit.convertAndSend(RabbitNames.EX_PROVISIONING, RabbitNames.RK_STAGE3, stream);


    }

}
