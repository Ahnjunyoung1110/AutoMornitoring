package AutoMonitoring.AutoMonitoring.domain.db.mqworker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.db.adapter.ProgramService;
import AutoMonitoring.AutoMonitoring.domain.db.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.dto.DbCommand;
import AutoMonitoring.AutoMonitoring.domain.dto.ProbeCommand;
import AutoMonitoring.AutoMonitoring.domain.dto.ProbeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbWorker {
    private final ProgramService programService;


    // 일딴은 insert만 구현
    @RabbitListener(queues = RabbitNames.Q_STAGE2)
    public void handle(DbCommand cmd){
        // db 에 저장한 후 redis의 상태를 변경
        // redis 저장은 추후 구현 예정
        Program program = Program.fromDto(cmd.probeDTO());
        programService.saveProgram(program);


    }

}
