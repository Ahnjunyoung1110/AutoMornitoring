package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.GZIPInputStream;


@Service
@Slf4j
@RequiredArgsConstructor
public class GetMediaServiceImpl implements GetMediaService {

    private final HttpClient httpClient;

    @Override
    public String getMedia(String url, String userAgent) {

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate") // 여러 인코딩 방식 추가
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7") // 브라우저와 동일한 언어 설정
                .header("Cache-Control", "no-cache") // 브라우저와 동일하게 캐시 관련 헤더 추가
                .header("Pragma", "no-cache") // 캐시를 무시하고 최신 데이터를 받기 위해
                .header("User-Agent", userAgent != null ? userAgent : "Mozilla/5.0 (Web0S; Linux; Tizen) AppleWebKit/537.36 (KHTML, like Gecko) SmartTV/1.0")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());

            int sc = res.statusCode();
            if (sc < 200 || sc >= 300) {
                throw new IOException("HTTP " + sc + " from " + url);
            }

            return decodeBody(res.body(),res.headers());
        } catch (IOException e) {
            throw new UncheckedIOException("GET failed: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GET interrupted: " + url, e);
        }
    }

    private static String decodeBody(byte[] body, HttpHeaders headers) throws IOException {
        Optional<String> enc = headers.firstValue("Content-Encoding");
        if (enc.isPresent() && enc.get().toLowerCase(Locale.ROOT).contains("gzip")) {
            try (GZIPInputStream gis = new GZIPInputStream(new java.io.ByteArrayInputStream(body));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                gis.transferTo(baos);
                return baos.toString(StandardCharsets.UTF_8);
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
