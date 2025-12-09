package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.SaveM3u8OptionCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.QueueSystemConfigCommand;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util.MonitoringConfigHolder;
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
    private final MonitoringConfigHolder configHolder;

    public void setOptions(SaveM3u8OptionCommand command){
        log.info("traceId{} 에 대한 저장 옵션 변경입니다. to {} ",command.traceId(), command.saveM3u8State());

        String key = RedisKeys.argument_record_discontinuity(command.traceId());

        redisService.setValues(key, command.saveM3u8State().toString());
    }


    // 시스템 스냅샷 변경 함수
    public void setSystemOption(QueueSystemConfigCommand c) {
        configHolder.updateConfig(c);
    }
}
