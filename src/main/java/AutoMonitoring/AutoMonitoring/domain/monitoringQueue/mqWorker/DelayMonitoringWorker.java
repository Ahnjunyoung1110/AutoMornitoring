package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class DelayMonitoringWorker {

    private final RedisService redisService;
    private final HttpClient httpClient;
    private final RabbitTemplate rabbit;

    // 메시지를 받아 처리하는 함수, 처리에 실패하면 1초 후 다시 큐에 넣는다.
    @RabbitListener(id = "Retry_queue",queues = RabbitNames.Q_WORK_DLX)
    void receiveMessage(Message m , CheckMediaManifestCmd cmd){
        // x-death 헤더에서 재시도 횟수를 계산 (0-indexed 이므로 0부터 시작)
        int attempt = deathCountForQueue(m, RabbitNames.Q_RETRY_DELAY_1S);
        String redisKey = RedisKeys.state(cmd.traceId(), cmd.resolution());

        // 최대 재시도 횟수 확인 (0,1,2,3 -> 4번 재시도 후 이번이 5번째 시도)
        if( attempt >= 4){
            log.warn("최대 재시도 횟수(5회)를 초과하여 최종 실패 처리합니다. TraceId: {}, Resolution: {}", cmd.traceId(), cmd.resolution());
            // 1. 최종 FAILED 상태 기록
            redisService.setValues(redisKey, "FAILED");
            throw new AmqpRejectAndDontRequeueException("5회 이상 재시도 실패");
        }

        String lockKey = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());
        Duration ttl = Duration.ofMinutes(3L);
        boolean lockAcquired = redisService.getOpsAbsent(lockKey, "1", ttl);

        if (!lockAcquired) {
            log.info("다른 워커가 이미 작업을 수행 중입니다. Key: %s".formatted(lockKey));
            return;
        }

        try{
            int currentAttempt = attempt + 2; // 사용자에게 보여줄 재시도 횟수 (2/5 부터 시작)
            log.info("재시도 요청을 수행합니다. (시도: {}/5)", currentAttempt);

            HttpRequest req = HttpRequest.newBuilder(URI.create(cmd.mediaUrl()))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .header("priority", "u=1, i")
                    .header("User-Agent", cmd.userAgent() != null ? cmd.userAgent() : "Mozilla/5.0 (Web0S; Linux; Tizen) AppleWebKit/537.36 (KHTML, like Gecko) SmartTV/1.0")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();

            if (code < 200 || code >= 400){
                throw new RuntimeException("재시도 실패. HTTP Status: " + code);
            }

            log.info("재시도 성공. 상태를 다시 MONITORING으로 변경하고 모니터링 큐로 보냅니다.");
            // 재시도 성공 시, 다시 MONITORING 상태로 변경
            redisService.setValues(redisKey, "MONITORING");
            rabbit.convertAndSend(RabbitNames.EX_MONITORING, RabbitNames.RK_WORK, cmd);
        }
        catch (Exception e){
            int nextAttempt = attempt + 2;
            log.warn("재시도 실패 ({}). 1초 후 다시 시도합니다. Error: {}", nextAttempt, e.getMessage());

            // 2. 다음 재시도 상태를 Redis에 기록
            String nextState = String.format("RETRYING (%d/5)", nextAttempt);
            redisService.setValues(redisKey, nextState);

            // 실패 시, EX_DELAY를 통해 재시도 딜레이 큐로 메시지를 보냄
            rabbit.convertAndSend(RabbitNames.EX_DELAY, RabbitNames.RK_RETRY_DELAY_1S, MessageBuilder
                    .withBody(m.getBody())
                    .andProperties(
                            MessagePropertiesBuilder
                                    .fromClonedProperties(m.getMessageProperties())
                                    .build()
                    )
                    .build());
        }
        finally {
            redisService.deleteValues(lockKey);
            log.info("재시도 작업 완료 후 Lock을 해제합니다. Key: %s".formatted(lockKey));
        }
    }


    // 메시지의 x-death 헤더를 확인하여 특정 큐에서 실패한 횟수를 계산
    static int deathCountForQueue(Message m, String queueName) {
        Object xdeath = m.getMessageProperties().getHeaders().get("x-death");
        if (!(xdeath instanceof List<?> list) || list.isEmpty()) return 0;
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(e -> queueName.equals(e.get("queue")))
                .findFirst()
                .map(e -> ((Number) e.getOrDefault("count", 0)).intValue())
                .orElse(0);
    }
}
