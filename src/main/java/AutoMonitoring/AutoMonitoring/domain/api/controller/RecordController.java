package AutoMonitoring.AutoMonitoring.domain.api.controller;

import AutoMonitoring.AutoMonitoring.domain.api.adapter.RecordManifest;
import AutoMonitoring.AutoMonitoring.domain.api.dto.ProbeAPI;
import AutoMonitoring.AutoMonitoring.domain.api.dto.ProbeRequestDTO;
import AutoMonitoring.AutoMonitoring.domain.api.service.UrlValidateCheck;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/probe")
public class RecordController {

    private final RecordManifest recordManifest;
    private final UrlValidateCheck urlValidateCheck;
    private final RedisService redisService;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody ProbeRequestDTO probeRequestDTO){
        String MasterManifestUrl = probeRequestDTO.getUrl();
        if (MasterManifestUrl.isEmpty()){
            return ResponseEntity.badRequest().body("Url이 입력되지 않았습니다..");
        }

        String userAgent = probeRequestDTO.getUserAgent() == null ? "" : probeRequestDTO.getUserAgent();
        String url = MasterManifestUrl.replaceAll("^\"|\"$", ""); // 양쪽 쌍따옴표 제거 방지 코드
        if (!url.startsWith("http")){
            url = url.trim();
            String StandardAds = "https://ads.its-newid.net";
            url = StandardAds + url;
        }

        // 해당 url 이 존재하는지 확인. 존재하지 않는다면 바로 fail 리턴
        boolean check = urlValidateCheck.check(url);
        if (!check){
            return ResponseEntity.badRequest().body("Url이 잘못 입력 되었습니다.");
        }

        String traceId = recordManifest.recordMasterManifest(url, userAgent);
        redisService.setValues(traceId, "TRYING");

        ProbeAPI probeAPI = ProbeAPI.builder()
                .traceId(traceId)
                .masterUrl(url)
                .build();
        return ResponseEntity.ok().body(probeAPI);
    }


    @PostMapping("/{traceId}/refresh")
    public ResponseEntity<Void> refresh(@PathVariable String traceId){

        recordManifest.refreshMonitoring(traceId);
        return ResponseEntity.ok().build();
    }

}
