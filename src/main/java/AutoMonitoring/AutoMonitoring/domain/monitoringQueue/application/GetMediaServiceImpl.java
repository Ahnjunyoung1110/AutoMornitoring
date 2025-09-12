package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
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
public class GetMediaServiceImpl implements GetMediaService {
    @Override
    public String getMedia(String url, String userAgnet) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(7))
                .header("User-Agent", "AutoMonitoring/1.0 (+hls-checker)")
                .header("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, text/plain, */*")
                // gzip을 보내줄 수 있으니 명시 (직접 해제 처리)
                .header("Accept-Encoding", "gzip")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

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
