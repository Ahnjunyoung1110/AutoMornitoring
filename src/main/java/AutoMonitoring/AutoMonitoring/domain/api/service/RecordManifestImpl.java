package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.mqWorker.ProbePubliser;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.dto.ProbeCommand;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordManifestImpl implements RecordManifest {


    private final ProbePubliser probePubliser;
    private final RedisService redis;

    @Override
    // traceId를 생성하고 이를 RabbitMQ에 입력
    public String recordMasterManifest(String MasterManifestUrl) {
        String traceId = java.util.UUID.randomUUID().toString().replace("-","");
        probePubliser.publish(new ProbeCommand(traceId, MasterManifestUrl));

        // redis에 작업이 진행중임을 기록
        redis.setValues(traceId, "Saving Data");


        return traceId;
    }
}
