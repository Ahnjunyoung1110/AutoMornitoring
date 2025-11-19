package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.contract.program.ProgramRefreshRequestCommand;
import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.mqWorker.ProbePublisher;
import AutoMonitoring.AutoMonitoring.domain.api.mqWorker.ProgramPublisher;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.ProbeCommand;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordManifestImpl implements RecordManifest {


    private final ProbePublisher probePublisher;
    private final ProgramPublisher programPublisher;
    private final RedisService redis;

    @Override
    // traceId를 생성하고 이를 RabbitMQ에 입력
    public String recordMasterManifest(String MasterManifestUrl, String UserAgent) {
        String traceId = java.util.UUID.randomUUID().toString().replace("-","");
        probePublisher.publish(new ProbeCommand(traceId, MasterManifestUrl, UserAgent));

        // redis에 작업이 진행중임을 기록
        redis.setValues(traceId, "Saving Data");


        return traceId;
    }


    // 같은 url + userAgent로 모니터링을 갱신한다.
    @Override
    public void refreshMonitoring(String traceId) {
        ProgramRefreshRequestCommand command = new ProgramRefreshRequestCommand(traceId);
        programPublisher.publish(command);

    }

}
