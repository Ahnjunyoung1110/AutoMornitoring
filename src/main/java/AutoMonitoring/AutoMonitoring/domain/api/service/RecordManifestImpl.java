package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.mqWorker.ProbePubliser;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.dto.ProbeCommand;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordManifestImpl implements RecordManifest {


    private final ProbePubliser probePubliser;
    private final RedisService redis;

    @Override
    // traceId를 생성하고 이를 RabbitMQ에 입력
    public String recordMasterManifest(String MasterManifestUrl, String UserAgent) {
        String traceId = java.util.UUID.randomUUID().toString().replace("-","");
        probePubliser.publish(new ProbeCommand(traceId, MasterManifestUrl, UserAgent));

        // redis에 작업이 진행중임을 기록
        redis.setValues(traceId, "Saving Data");


        return traceId;
    }

    @Override
    // 해당 traceId 에 대해서 광고가 나온 구간의 메니페스트를 저장 필요를 저장한다.
    public void recordAdLog(String traceId, Boolean recordAdLog){
        String key = RedisKeys.argument_record_discontinuity(traceId);
        if (redis.getValues(traceId).startsWith("false")){
            throw new IllegalArgumentException("해당 traceId가 존재하지 않습니다.");
        }


        redis.setValues(key, recordAdLog.toString());
    }
}
