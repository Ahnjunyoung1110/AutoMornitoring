package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.util.CheckValidConfigHolder;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public final class AlarmService {

    private final WebClient webClient;
    private final CheckValidConfigHolder configHolder;
    private final RedisService redisService; // RedisService 주입

    @Value("${slack.webhook-url}")
    String url;

    public void publishAlarm(String validationResult, String resolution, String traceId) {
        if (!configHolder.getAlarmEnabled().get()) {
            log.info("Alarm publishing is disabled. Skipping alarm for traceId: {}", traceId);
            return;
        }

        if (url.isEmpty()){
            log.info("알람 경로가 지정되지 않았습니다.");
            return;
        }

        // 1. 쿨다운 체크
        String cooldownKey = RedisKeys.alarmCooldownKey(traceId, resolution);
        if (!redisService.getValues(cooldownKey).contains("false")) {
            log.info("Alarm is in cooldown for traceId: {}, resolution: {}. Skipping.", traceId, resolution);
            return;
        }

        // 2. 임계값(Threshold) 체크 - 30분 내에 일정 횟수 이상 발생 시 알람 발송
        String countKey = RedisKeys.alarmCountKey(traceId, resolution);
        int currentCount = redisService.increment(countKey, 1L).intValue();
        // 첫 WARN인 경우 1시간 마다 횟수가 초기화 되도록 설정
        if(currentCount == 1){
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            ZonedDateTime nextHour = now
                    .plusHours(1)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            Duration ttl = Duration.between(now, nextHour);
            redisService.expire(countKey, ttl);
        }

        // 임계값 확인
        int threshold = configHolder.getThreshold().get();
        if (currentCount < threshold) {
            log.info("아직 threshold 를 넘지 않았습니다.: {}, resolution: {}. Current count: {}, Threshold: {}. Skipping.",
                    traceId, resolution, currentCount, threshold);
            return;
        }


        // slack 에 표현할 payload 생성
        Map<String, Object> payload = buildPayload(validationResult, resolution, traceId);


        // 비동기 post
        webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(body -> log.debug("Slack response: {}", body))
                .doOnError(e -> log.error("Slack webhook failed", e))
                .then().subscribe();

        // 알람 발송 후 쿨다운 설정 및 초기화
        redisService.setValues(cooldownKey, "1");
        redisService.expire(cooldownKey, Duration.ofSeconds(configHolder.getAlarmCooldownSeconds().get()));
        redisService.expire(countKey, Duration.ZERO);
    }


    // 슬랙에 표시할 payload 생성
    private Map<String, Object> buildPayload(String validationResult, String resolution, String traceId) {
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // ----- fields -----
        Map<String, Object> resultField = new HashMap<>();
        resultField.put("title", "검증 결과");
        resultField.put("value", "```" + validationResult + "```");
        resultField.put("short", false); // 한 줄로 안 붙이게

        Map<String, Object> resolutionField = new HashMap<>();
        resolutionField.put("title", "resolution");
        resolutionField.put("value", resolution);
        resolutionField.put("short", false);

        Map<String, Object> traceIdField = new HashMap<>();
        traceIdField.put("title", "Trace ID");
        traceIdField.put("value", "`" + traceId + "`");
        traceIdField.put("short", false);

        // ----- attachment -----
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", "#FF4D4F");
        attachment.put("pretext", ":rotating_light: *미디어 유효성 검증 알람* :rotating_light:");
        attachment.put("fields", List.of(resultField, resolutionField, traceIdField));
        attachment.put("footer", "AutoMonitoring · " + now);

        // ----- top-level payload -----
        Map<String, Object> payload = new HashMap<>();
        payload.put("text", ":this_is_fine: :pepe-nervous: 미디어 유효성 검증 알람 발생 :pepe-nervous: :this_is_fine:");
        payload.put("attachments", List.of(attachment));

        return payload;
    }


}
