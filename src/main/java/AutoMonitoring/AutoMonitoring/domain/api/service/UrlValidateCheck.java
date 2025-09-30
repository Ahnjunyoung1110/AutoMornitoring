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

    // 주어진 URL로 HEAD 요청을 보내고 2초 이내에 정상 응답이면 true, 아니면 falsermsep
    public boolean check(String paramUrl){
        try{

            String url = paramUrl.trim();
            // 인코딩 된 문장인지 확인 안되어있다면 인코딩
            if (!url.matches(".*%[0-9A-Fa-f]{2}.*")){
                url = url.replace( "[", "%5B").replace(  "]", "%5D");
                url = UriComponentsBuilder.fromUriString(url).build(false).toUriString();
            }
            // '[', ']' 는 인코딩 되면 요청이 안되므로 재 수정
            String escapedUrl = url.replace( "%5B","[").replace( "%5D", "]");

            HttpRequest request = HttpRequest.newBuilder(URI.create(escapedUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();

            long t0 = System.nanoTime();

            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

            if (elapsedMs > 4000) return false;
            int code  = response.statusCode();
            return code >= 200 && code < 400;

        } catch (Exception e){
            return false;
        }
    }
}
