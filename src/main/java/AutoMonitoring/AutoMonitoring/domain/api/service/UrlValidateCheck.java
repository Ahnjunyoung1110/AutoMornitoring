package AutoMonitoring.AutoMonitoring.domain.api.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
/*  정상적으로 파일이 있는지 확인하는 함수 */
public class UrlValidateCheck {
    private final HttpClient http;

    // 주어진 URL로 HEAD 요청을 보내고 5초 이내에 정상 응답이면 true, 아니면 falsermsep
    public boolean check(String paramUrl){
        try {
            String url = paramUrl.trim();

            // 대괄호 처리를 포함한 안전 인코딩
            if (!url.matches(".*%[0-9A-Fa-f]{2}.*")) {
                url = url.replace("[", "%5B").replace("]", "%5D");
                url = UriComponentsBuilder.fromUriString(url).build(false).toUriString();
            }
            // 일부 엔드포인트 호환 위해 대괄호 원복
            String escapedUrl = url.replace("%5B", "[").replace("%5D", "]");

            // HttpClient는 재사용(싱글턴) 권장. 여기선 예제 편의상 생성
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder(URI.create(escapedUrl))
                    .method("GET", HttpRequest.BodyPublishers.noBody())   // GET(본문 없음)
                    .timeout(Duration.ofSeconds(5))
                    // 전송량 최소화(가능하면 압축 끄기, 바디 1바이트만 요청 시도)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("Range", "bytes=0-0")
                    // 일부 CDN/Origin이 UA 없으면 4xx를 줄 수 있음
                    .header("User-Agent", "AutoMonitoring/1.0")
                    .build();

            long t0 = System.nanoTime();
            HttpResponse<Void> resp = http.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

            if (elapsedMs > 5000) return false;            // 타임아웃 가드(5s)
            int code = resp.statusCode();
            return (code >= 200 && code < 400);

        } catch (Exception e) {
            log.error("헤더 체크 실패: {}", e.toString());
            return false;
        }
    }
}
