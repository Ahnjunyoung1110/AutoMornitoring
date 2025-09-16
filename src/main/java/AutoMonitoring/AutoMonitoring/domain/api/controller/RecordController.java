package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.dto.ProbeAPI;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/probe")
public class RecordController {

    private final RecordManifest recordManifest;
    @PostMapping
    public ResponseEntity<ProbeAPI> submit(@RequestParam String MasterManifestUrl,
                                           @RequestParam
                                           @Nullable String UserAgent){
        String url = MasterManifestUrl.replaceAll("^\"|\"$", ""); // 양쪽 쌍따옴표 제거 방지 코드

        String traceId = recordManifest.recordMasterManifest(url, UserAgent);
        ProbeAPI probeAPI = ProbeAPI.builder()
                .traceId(traceId)
                .masterUrl(url)
                .build();
        return ResponseEntity.ok().body(probeAPI);

    }
}
