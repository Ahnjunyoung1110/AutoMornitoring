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

            if (!url.matches(".*%[0-9A-Fa-f]{2}.*")) {
                url = url.replace("[", "%5B").replace("]", "%5D");
                url = UriComponentsBuilder.fromUriString(url).build(false).toUriString();
            }
            String escapedUrl = url.replace("%5B", "[").replace("%5D", "]");

            // ★ 여기서 ../, ./ 정규화
            URI rawUri = URI.create(escapedUrl);
            URI normalizedUri = rawUri.normalize();

            // Use the injected HttpClient
            HttpRequest request = HttpRequest.newBuilder(normalizedUri)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("Range", "bytes=0-0")
                    .header("User-Agent", "AutoMonitoring/1.0")
                    .build();

            HttpResponse<Void> resp = http.send(request, HttpResponse.BodyHandlers.discarding());

            int code = resp.statusCode();
            log.info("UrlValidateCheck status={} url={} normalized={}", code, escapedUrl, normalizedUri);
            return (code >= 200 && code < 400);

        } catch (Exception e) {
            log.error("헤더 체크 실패: {}", e.toString());
            return false;
        }
    }
}
