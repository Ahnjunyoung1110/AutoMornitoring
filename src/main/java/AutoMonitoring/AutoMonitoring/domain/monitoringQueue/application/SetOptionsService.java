package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.SaveM3u8OptionCommand;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SetOptionsService {

    private final RedisService redisService;

    public void setOptions(SaveM3u8OptionCommand command){
        log.info("traceId{} 에 대한 저장 옵션 변경입니다. to {} ",command.traceId(), command.saveM3u8State());

        String key =RedisKeys.argument_record_discontinuity(command.traceId());

        redisService.setValues(key, command.saveM3u8State().toString());
    }

}
