package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UrlValidateCheck {
    private static final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(1500)) // 연결 지연 방지
            .build();

    // 주어진 URL로 HEAD 요청을 보내고 2초 이내에 정상 응답이면 true, 아니면 falsermsep
    public boolean check(String manifestUrl){
        try{
            HttpRequest request = HttpRequest.newBuilder(URI.create(manifestUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(2))
                    .header("User-Agent", "AutoMonitoring/1.0 (+check-head)")
                    .build();

            long t0 = System.nanoTime();

            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

            if (elapsedMs > 2000) return false;
            int code  = response.statusCode();
            return code >= 200 && code < 400;

        } catch (Exception e){
            return false;
        }
    }
}
