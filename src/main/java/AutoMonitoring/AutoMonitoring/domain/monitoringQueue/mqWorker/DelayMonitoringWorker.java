package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception.TooManyFailException;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @RabbitListener(queues = RabbitNames.WORK_DLX_QUEUE)
    void receiveMessage(Message m , CheckMediaManifestCmd cmd){
        // 헤더 확인 5회 이상이면 방출 후 알람
        int attemp = deathCountForQueue(m, RabbitNames.ONLY_DELAY_QUEUE_1S_DELAY);
        if( attemp >= 5){
            // 방출 알람 구현 예정
            log.warn("이젠 가라.");
            throw new TooManyFailException("5회 이상 실패 했습니다.");
        }

        // 알람 도메인으로 가는 rabbitMQ 코드

        String lockKey = RedisKeys.workerLock(cmd.traceId(), cmd.resolution());
        Duration ttl = Duration.ofMinutes(3L); // 워커가 비정상 종료될 경우를 대비한 TTL
        boolean lockAcquired = redisService.getOpsAbsent(lockKey, "1", ttl);

        if (!lockAcquired) {
            log.info("다른 워커가 이미 작업을 수행 중입니다. Key: %s".formatted(lockKey));
            return; // Lock 획득에 실패하면 중복 작업이므로 즉시 종료
        }

        try{
            log.info("아리까리 한놈 요청 해봅니다잉.");

            // curl
            HttpRequest req = HttpRequest.newBuilder(URI.create(cmd.mediaUrl()))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip, deflate") // 여러 인코딩 방식 추가
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7") // 브라우저와 동일한 언어 설정
                    .header("Cache-Control", "no-cache") // 브라우저와 동일하게 캐시 관련 헤더 추가
                    .header("Pragma", "no-cache") // 캐시를 무시하고 최신 데이터를 받기 위해
                    .header("priority", "u=1, i")
                    .header("User-Agent", cmd.userAgent() != null ? cmd.userAgent() : "Mozilla/5.0 (Web0S; Linux; Tizen) AppleWebKit/537.36 (KHTML, like Gecko) SmartTV/1.0")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();

            if (code< 200 || code >= 400){
                throw new RuntimeException("에러 났대요!!!!!!! " + code);
            }

            log.info("다시 받아보니 정상적이넹");
            rabbit.convertAndSend(RabbitNames.EX_PIPELINE, RabbitNames.WORK_QUEUE, cmd);
        }
        catch (Exception e){
            log.warn("이놈 봐라.");
            
            // 헤더를 전부 유지하면서 전송
            rabbit.convertAndSend("", RabbitNames.ONLY_DELAY_QUEUE_1S_DELAY, MessageBuilder
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
            log.info("작업 완료 후 Lock을 해제합니다. Key: %s".formatted(lockKey));
        }
    }


    // 메시지의 실패 횟수를 확인하고 돌려준다.
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
    // 실패하는 횟수가 threshold(4) 를 넘어간 url을 처리한다.
//    boolean recordFailUrl(){}


}
