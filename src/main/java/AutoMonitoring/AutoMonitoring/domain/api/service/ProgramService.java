package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.contract.program.ProgramOptionCommand;
import AutoMonitoring.AutoMonitoring.domain.api.mqWorker.ProgramPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

    private final ProgramPublisher programPublisher;

    // 해당 traceId 에 대해서 m3u8 저장 옵션을 지정하고 메시지를 발행한다.
    public void setOptions(ProgramOptionCommand command){
        log.info("옵션을 변경합니다. {}",command.toString());
        programPublisher.publish(command);
    }
}
