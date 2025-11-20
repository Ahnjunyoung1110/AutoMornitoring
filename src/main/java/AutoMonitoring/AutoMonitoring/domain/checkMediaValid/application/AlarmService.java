package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import com.slack.api.Slack;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public final class AlarmService {
    @Value("${slack.webhook-url}")
    String url;

    public void PublishAlarm(String validationResult, String resolution, String traceId){
        Slack slack = Slack.getInstance();

        // 메인 알림 색상 / 필드 구성
        Attachment attachment = Attachment.builder()
                .color("#FF4D4F") // 빨간색 알림 바
                .pretext(":rotating_light: *미디어 유효성 검증 알람* :rotating_light:")
                .fields(List.of(
                        Field.builder()
                                .title("검증 결과")
                                .value("```" + validationResult + "```")
                                .build(),
                        Field.builder()
                                .title("resolution")
                                .value(resolution)
                                .build(),
                        Field.builder()
                                .title("Trace ID")
                                .value("`" + traceId + "`")
                                .build()
                ))
                .footer("AutoMonitoring · " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();

        Payload payload = Payload.builder()
                // 모바일 푸시/알림 미리보기용 한 줄
                .text(":this_is_fine: :pepe-nervous: 미디어 유효성 검증 알람 발생 :pepe-nervous: :this_is_fine:")
                .attachments(Collections.singletonList(attachment))
                .build();

        try {


            WebhookResponse response =
                    slack.send(url, payload);

            int status = response.getCode();
            if (status / 100 != 2) {
                log.warn("Slack webhook failed. status={}, body={}", status, response.getBody());
            } else {
                log.debug("Slack webhook success. status={}", status);
            }
        } catch (Exception e) {
            log.error("Failed to send Slack alarm", e);
        }
    }
}
