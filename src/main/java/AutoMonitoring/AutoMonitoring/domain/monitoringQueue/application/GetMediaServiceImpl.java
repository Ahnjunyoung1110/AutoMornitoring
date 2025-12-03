package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.GetMediaService;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception.SessionExpiredException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetMediaServiceImpl implements GetMediaService {

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Web0S; Linux; Tizen) AppleWebKit/537.36 (KHTML, like Gecko) SmartTV/1.0";

    private final HttpClient httpClient;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getMedia(String url, String userAgent, String traceId) {

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("User-Agent", userAgent != null ? userAgent : DEFAULT_USER_AGENT)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> res =
                    httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());

            String contentEncoding =
                    res.headers().firstValue("Content-Encoding").orElse(null);

            // 상태코드 / 바디 / 헤더 기반으로 예외 판단 (필요하면 여기서 바로 던짐)
            validateResponse(res, contentEncoding, traceId);

            // 예외가 안 던져졌으면 정상 응답으로 간주
            return decodeBody(res.body(), contentEncoding);

        } catch (IOException e) {
            // I/O (디코딩, HTTP 통신 등) 문제
            throw new UncheckedIOException("GET failed: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GET interrupted: " + url, e);
        } catch (SessionExpiredException e) {
            throw e;
        }
    }

    @Override
    public Mono<String> getMediaNonBlocking(String url, String userAgent, String traceId) {
        URI normalizedUri = URI.create(url.trim()).normalize();
        return webClient.get()
                .uri(normalizedUri)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("User-Agent", userAgent != null ? userAgent : DEFAULT_USER_AGENT)
                .exchangeToMono(clientResponse -> clientResponse
                        .toEntity(byte[].class)
                        .flatMap(entity -> {
                            String contentEncoding = entity.getHeaders().getFirst("Content-Encoding");

                            try {
                                // 동기 쪽과 동일한 검증 로직 호출
                                validateReactiveResponse(
                                        entity.getStatusCode().value(),
                                        entity.getHeaders(),
                                        entity.getBody(),
                                        contentEncoding,
                                        traceId
                                );
                                String decoded = decodeBody(entity.getBody(), contentEncoding);
                                return Mono.just(decoded);
                            } catch (SessionExpiredException e) {
                                return Mono.error(e);
                            } catch (IOException e) {
                                return Mono.error(new UncheckedIOException(e));
                            }
                        })
                );
    }

    /**
     * GZIP 여부에 따라 바디 디코딩
     */
    private static String decodeBody(byte[] body, String contentEncoding) throws IOException {
        if (body == null) {
            return "";
        }

        if (contentEncoding != null &&
                contentEncoding.toLowerCase(Locale.ROOT).contains("gzip")) {

            try (GZIPInputStream gis =
                         new GZIPInputStream(new java.io.ByteArrayInputStream(body));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                gis.transferTo(baos);
                return baos.toString(StandardCharsets.UTF_8);
            }
        }

        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * 상태 코드 + 헤더 + 바디를 보고 예외를 직접 던진다.
     * - 정상(2xx, 3xx) → 아무 것도 안 던짐
     * - aniview 세션 만료 → SessionExpiredException
     * - Publica 세션 만료 → SessionExpiredException
     * - 그 외 4xx/5xx → IOException
     */
    private void validateResponse(HttpResponse<byte[]> res,
                                  String contentEncoding, String traceId) throws IOException {

        int status = res.statusCode();

        // 1) 2xx / 3xx 는 예외 아님
        if (status < 400) {
            return;
        }

        // 2) 퍼블리카 헤더 (세션 만료 케이스용)
        String publicaErr = res.headers()
                .firstValue("Publica-Error-Message")
                .orElse(null);

        // 3) 400 Bad Request 계열 처리
        if (status == 400) {

            // 3-1) aniview: 바디 JSON 기준 세션 만료
            byte[] body = res.body();
            if (body != null && body.length > 0) {
                String decodedBody = decodeBody(body, contentEncoding);

                try {
                    JsonNode root = objectMapper.readTree(decodedBody);

                    String error = root.path("error").asText(null);
                    String description = root.path("description").asText(null);

                    if ("SES".equals(error)
                            && "session not found".equalsIgnoreCase(description)) {
                        // aniview 세션 만료 확정
                        throw new SessionExpiredException(
                                "Aniview session expired: " + decodedBody , traceId
                        );
                    }
                } catch (Exception e) {
                    // JSON 아니면 aniview 세션 만료 패턴 아님 → 무시하고 다른 케이스로 진행
                }
            }

            // 3-2) Publica: 헤더 기반 세션 만료
            if (publicaErr != null
                    && publicaErr.contains("failed retrieve state from cache")) {
                throw new SessionExpiredException(
                        "Publica session expired: " + publicaErr, traceId
                );
            }

            // 3-3) 여기까지 안 걸리면 그냥 일반적인 400 에러로 본다
            throw new IOException("HTTP 400 from " + res.uri()
                    + " (Publica-Error-Message=" + publicaErr + ")");
        }

        // 4) 나머지 4xx / 5xx 일반 에러
        if (status >= 400 && status < 500) {
            throw new IOException("HTTP " + status + " from " + res.uri()
                    + " (4xx, Publica-Error-Message=" + publicaErr + ")");
        }

        if (status >= 500) {
            throw new IOException("HTTP " + status + " from " + res.uri()
                    + " (5xx, Publica-Error-Message=" + publicaErr + ")");
        }

        // 이론상 여기 안 옴
        throw new IOException("Unexpected HTTP " + status + " from " + res.uri());
    }

    private void validateReactiveResponse(
            int status,
            HttpHeaders headers,
            byte[] body,
            String contentEncoding,
            String traceId
    ) throws IOException {

        if (status < 400) return;

        String publicaErr = headers.getFirst("Publica-Error-Message");

        // 400: aniview
        if (status == 400 && body != null && body.length > 0) {
            String decoded = decodeBody(body, contentEncoding);
            String trimmed = decoded.trim();

            // json 이 아니면 패싱
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                JsonNode root = objectMapper.readTree(decoded);
                String error = root.path("error").asText(null);
                String description = root.path("description").asText(null);

                if ("SES".equals(error)
                        && "session not found".equalsIgnoreCase(description)) {
                    throw new SessionExpiredException(
                            "Aniview session expired: " + decoded, traceId
                    );
                }
            }
        }

        // Publica
        if (publicaErr != null &&
                publicaErr.contains("failed retrieve state from cache")) {
            throw new SessionExpiredException(
                    "Publica session expired: " + publicaErr, traceId
            );
        }

        throw new IOException("HTTP " + status + " (Reactive) – URL Session issue? traceId=" + traceId);
    }



}
