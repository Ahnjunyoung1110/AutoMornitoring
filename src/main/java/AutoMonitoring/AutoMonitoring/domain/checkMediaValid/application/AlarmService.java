package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public final class AlarmService {

    private final WebClient webClient;

    @Value("${slack.webhook-url}")
    String url;

    public void publishAlarm(String validationResult, String resolution, String traceId) {
        Map<String, Object> payload = buildPayload(validationResult, resolution, traceId);

        webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(body -> log.debug("Slack response: {}", body))
                .doOnError(e -> log.error("Slack webhook failed", e))
                .then().subscribe();
    }

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
