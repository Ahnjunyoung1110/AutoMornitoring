package AutoMonitoring.AutoMonitoring.domain.api.service;

import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.mqWorker.ProbePubliser;
import AutoMonitoring.AutoMonitoring.domain.dto.ProbeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordManifestImpl implements RecordManifest {


    private final ProbePubliser probePubliser;

    @Override
    // traceId를 생성하고 이를 RabbitMQ에 입력
    public String recordMasterManifest(String MasterManifestUrl) {
        String traceId = java.util.UUID.randomUUID().toString().replace("-","");
        probePubliser.publish(new ProbeCommand(traceId, MasterManifestUrl));

        return traceId;
    }
}
