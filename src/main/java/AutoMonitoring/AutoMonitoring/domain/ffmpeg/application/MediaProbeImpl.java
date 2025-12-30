package AutoMonitoring.AutoMonitoring.domain.ffmpeg.application;

import AutoMonitoring.AutoMonitoring.contract.ffmpeg.FfmpegCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProbeDTO;
import AutoMonitoring.AutoMonitoring.contract.program.StreamDTO;
import AutoMonitoring.AutoMonitoring.contract.program.VariantDTO;
import AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter.MediaProbe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaProbeImpl implements MediaProbe {
    private final ObjectMapper om;
    @Value("${ffmpeg.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Override
    public ProbeDTO probe(FfmpegCommand probeCommand) {
        try {
            // URL에서 쿼리 파라미터 추출
            Map<String, String> queryParams = Stream.of(probeCommand.masterUrl().split("[?&]"))
                    .filter(s -> s.contains("="))
                    .map(s -> s.split("=", 2))
                    .collect(Collectors.toMap(
                            a -> a[0],
                            a -> a.length > 1 ? a[1] : "",
                            (v1, v2) -> v1 // 중복 키의 경우 첫 번째 값을 사용
                    ));

            String channelName = queryParams.get("channel_name");
            String channelId = queryParams.get("channel_id");
            String tp = queryParams.get("tp");

            try {
                if (channelName != null) channelName = URLDecoder.decode(channelName, StandardCharsets.UTF_8);
                if (channelId != null) channelId = URLDecoder.decode(channelId, StandardCharsets.UTF_8);
                if (tp != null) tp = URLDecoder.decode(tp, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("URL 파라미터 디코딩에 실패했습니다.", e);
            }


            String url = probeCommand.masterUrl().trim();
            // 인코딩 된 문장인지 확인 안되어있다면 인코딩
            if (!url.matches(".*%[0-9A-Fa-f]{2}.*")){
                url = url.replace( "[", "%5B").replace(  "]", "%5D");
                url = UriComponentsBuilder.fromUriString(url).build(false).toUriString();
            }
            // '[', ']' 는 인코딩 되면 요청이 안되므로 재 수정
            String escapedUrl = url.replace( "%5B","[").replace( "%5D", "]");
            String formatName     = "";           // ex) "hls"
            Double durationSec    = null;  // 라이브면 null일 수 있음
            Integer overallBitrate= null;     // bps, 없을 수 있음
            List<StreamDTO> streamDTOs = new ArrayList<>();

            try{
                // 1) ffprobe 실행
                String json = run(
                        ffprobePath,
                        "-v","error",
                        "-show_error",
                        "-print_format","json",           // = -of json
                        "-show_format","-show_streams",
                        "-f","hls",                       // ★ HLS demuxer 강제
                        "-allowed_extensions","ALL",
                        "-rw_timeout", "10000000",                 // 10초
                        "-max_reload", "1",         // 플레이리스트를 1번만 리로드하고 포기
                        "-extension_picky", "0",
                        "-protocol_whitelist","file,http,https,tcp,tls,crypto", // ★ 내부 fetch 허용
                        "-i" , escapedUrl
                );

                // 2) JSON 파싱
                JsonNode root    = om.readTree(json);
                JsonNode fmt     = root.path("format");
                JsonNode streams = root.path("streams");

                formatName     = textOrNull(fmt, "format_name");           // ex) "hls"
                durationSec    = parseDouble(textOrNull(fmt, "duration"));  // 라이브면 null일 수 있음
                overallBitrate= parseInt(textOrNull(fmt, "bit_rate"));     // bps, 없을 수 있음

                // 3) streams → List<StreamDTO>


                if (streams.isArray()) {
                    for (JsonNode s : streams) {
                        String type = s.path("codec_type").asText(null);
                        String codec= s.path("codec_name").asText(null);

                        if ("video".equals(type)) {
                            Integer width  = numberOrNull(s, "width");
                            Integer height = numberOrNull(s, "height");
                            // r_frame_rate(예: "30/1") 또는 avg_frame_rate
                            Double fps = parseRatio(s.path("r_frame_rate").asText(null));
                            if (fps == null) fps = parseRatio(s.path("avg_frame_rate").asText(null));

                            StreamDTO dto = StreamDTO.builder()
                                    .type("video")
                                    .codec(codec)
                                    .width(width)
                                    .height(height)
                                    .fps(fps)
                                    .build();
                            streamDTOs.add(dto);
                        } else if ("audio".equals(type)) {
                            Integer channels = numberOrNull(s, "channels");
                            String lang = null;
                            if (s.has("tags")) lang = s.path("tags").path("language").asText(null);

                            StreamDTO dto = StreamDTO.builder()
                                    .type("audio")
                                    .codec(codec)
                                    .channels(channels)
                                    .lang(lang)
                                    .build();
                            streamDTOs.add(dto);
                        } else {
                            // 다른 타입(자막 등)은 지금은 무시. 필요하면 확장
                        }
                    }
                }
            } catch (Exception e){
                log.warn("ffprobe failed. continue without streams. url={}", escapedUrl, e);
            }


            // 4) 마스터 m3u8 파싱 → variants
            List<VariantDTO> variants = parseMasterM3u8(escapedUrl);

            // 5) ProbeDTO 생성
            return ProbeDTO.builder()
                    .traceId(probeCommand.traceId())
                    .masterManifestUrl(probeCommand.masterUrl())
                    .channelName(channelName)
                    .channelId(channelId)
                    .tp(tp)
                    .userAgent(probeCommand.userAgent())
                    .format(formatName)
                    .durationSec(durationSec)
                    .overallBitrate(overallBitrate)
                    .saveM3u8State(null) // 초기 상태는 null
                    .streams(streamDTOs)
                    .variants(variants)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("probe failed: " + e.getMessage(), e);
        }
    }

    /* ---------- 내부 유틸 ---------- */

    private static String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static Integer numberOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && n.isNumber()) ? n.asInt() : null;
    }

    // s가 null/공백이면 null, 숫자면 Double, 아니면 null
    private static Double parseDouble(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return Integer.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    // "30000/1001" or "30/1" → 29.97, 30.0 Double로 변경해주는 함수
    private static Double parseRatio(String s) {
        if (s == null || s.isBlank()) return null;
        int i = s.indexOf('/');
        if (i < 0) return parseDouble(s);
        try {
            double a = Double.parseDouble(s.substring(0, i));
            double b = Double.parseDouble(s.substring(i + 1));
            if (b == 0.0) return null;
            return a / b;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 실제 커맨드 런 함수
    private static String run(String... cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).start();

        var outBuf = new java.io.ByteArrayOutputStream();
        var errBuf = new java.io.ByteArrayOutputStream();

        Thread tOut = new Thread(() -> {
            try (var in = p.getInputStream()) {
                in.transferTo(outBuf);
            } catch (IOException ignored) {}
        });
        Thread tErr = new Thread(() -> {
            try (var in = p.getErrorStream()) {
                in.transferTo(errBuf);
            } catch (IOException ignored) {}
        });

        tOut.start();
        tErr.start();

        int code = p.waitFor();
        tOut.join();
        tErr.join();

        String out = outBuf.toString(UTF_8);
        String err = errBuf.toString(UTF_8);

        if (code != 0) {
            throw new IllegalArgumentException(
                    "probe failed: exit=" + code +
                            "\ncmd=" + String.join(" ", cmd) +
                            "\nstderr:\n" + err
            );
        }
        return out;
    }



    /* ---------- 마스터 M3U8 파서 (#EXT-X-STREAM-INF) ---------- */
    // 파싱후 서브 메니페스트의 url을 저장한다.

    private static final Pattern STREAM_INF_ATTR =
            Pattern.compile("(\\w+)=((?:\"[^\"]+\")|[^,]+)");

    private List<VariantDTO> parseMasterM3u8(String masterUrl) {

        log.info("함수가 실행되었습니다.");
        try {
            log.info("함수가 실행되었습니다1.");
            String cleaned = masterUrl.replace("\r","").replace("\n","").trim();
            String[] parts = cleaned.split("\\?");
            String baseUrl = parts[0];
            String finalUrl;

            // param이 있는 경우만
            if (parts.length == 2) {
                String queryParam = parts[1];
                String encodedQueryParams = UriEncoder.encode(queryParam);  // 인코딩된 쿼리 파라미터
                finalUrl = baseUrl + "?" + encodedQueryParams;
            } else {
                finalUrl = baseUrl;
            }


            log.info("함수가 실행되었습니다2.");




            HttpClient hc = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(finalUrl)).GET().build();
            String body = hc.send(req, HttpResponse.BodyHandlers.ofString()).body();

            List<String> lines = body.lines().toList();
            List<VariantDTO> out = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.startsWith("#EXT-X-STREAM-INF")) continue;

                // attributes
                Map<String, String> kv = new HashMap<>();
                Matcher m = STREAM_INF_ATTR.matcher(line.substring("#EXT-X-STREAM-INF:".length()));
                while (m.find()) {
                    String k = m.group(1);
                    String v = m.group(2);
                    if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                    kv.put(k, v);
                }

                // 다음 유효 라인이 URI
                String mediaUri = null;
                int j = i + 1;
                while (j < lines.size()) {
                    String nxt = lines.get(j).trim();
                    if (!nxt.isEmpty() && !nxt.startsWith("#")) {
                        mediaUri = nxt;
                        break;
                    }
                    j++;
                }
                if (mediaUri == null) continue;

                String resolution = kv.getOrDefault("RESOLUTION", null); // e.g. 1920x1080
                Integer bandwidth = null;
                if (kv.containsKey("BANDWIDTH")) {
                    try { bandwidth = Integer.valueOf(kv.get("BANDWIDTH"));
                    } catch (NumberFormatException ignore) {}
                }

                String audioGroup = kv.getOrDefault("AUDIO", null);


                // 절대 url이 아닌경우 반영
                URI media = URI.create(mediaUri);
                URI finalURL = URI.create(finalUrl);
                if (!media.isAbsolute()) {
                    // scheme-relative: //cdn.example.com/a.m3u8
                    if (mediaUri.startsWith("//")) {
                        String scheme = finalURL.getScheme(); // https
                        if (scheme == null) {
                            throw new IllegalArgumentException("Master URL scheme is null: " + finalUrl);
                        }
                        mediaUri = scheme + ":" + mediaUri;
                    } else {
                        // 일반 상대경로: CTSNewsQT_1.m3u8, sub/1.m3u8, ../1.m3u8, /root/1.m3u8
                        mediaUri = finalURL.resolve(mediaUri).toString();
                    }
                }

                out.add(new VariantDTO(resolution, bandwidth, mediaUri, audioGroup));
            }
            return out;
        } catch (Exception e) {
            // 네트워크 실패해도 probe 자체는 진행 가능하도록 빈 리스트 반환
            log.warn("실 패 했 다.");
            log.warn(e.getMessage());
            return List.of();


        }
    }

    static String buildEncodedUrl(String base, Map<String,String> params) {
        StringBuilder sb = new StringBuilder(base);
        sb.append(base.contains("?") ? "&" : "?");
        boolean first = true;
        for (var e : params.entrySet()) {
            if (!first) sb.append('&');
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), UTF_8));
        }
        return sb.toString();
    }

}
