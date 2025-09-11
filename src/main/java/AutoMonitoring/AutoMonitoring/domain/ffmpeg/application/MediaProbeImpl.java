package AutoMonitoring.AutoMonitoring.domain.ffmpeg.application;

import AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter.MediaProbe;
import AutoMonitoring.AutoMonitoring.domain.program.dto.ProbeDTO;
import AutoMonitoring.AutoMonitoring.domain.program.dto.StreamDTO;
import AutoMonitoring.AutoMonitoring.domain.program.dto.VariantDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MediaProbeImpl implements MediaProbe {
    private final ObjectMapper om;
    @Value("${ffmpeg.ffprobe-path:ffprobe}")
    private String ffprobePath;




    @Override
    public ProbeDTO probe(String masterManifestUrl) {
        try {
            // 1) ffprobe 실행
            String json = run(
                    ffprobePath, "-v", "error",
                    "-of", "json",
                    "-show_format", "-show_streams",
                    masterManifestUrl
            );

            // 2) JSON 파싱
            JsonNode root    = om.readTree(json);
            JsonNode fmt     = root.path("format");
            JsonNode streams = root.path("streams");

            String formatName     = textOrNull(fmt, "format_name");           // ex) "hls"
            Double durationSec    = parseDouble(textOrNull(fmt, "duration"));  // 라이브면 null일 수 있음
            Integer overallBitrate= parseInt(textOrNull(fmt, "bit_rate"));     // bps, 없을 수 있음

            // 3) streams → List<StreamDTO>
            List<StreamDTO> streamDTOs = new ArrayList<>();
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

            // 4) 마스터 m3u8 파싱 → variants
            List<VariantDTO> variants = parseMasterM3u8(masterManifestUrl);

            // 5) ProbeDTO 생성
            return new ProbeDTO(
                    java.util.UUID.randomUUID().toString().replace("-", ""),
                    Instant.now(),
                    masterManifestUrl,
                    formatName,
                    durationSec,
                    overallBitrate,
                    streamDTOs,
                    variants
            );
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
    private static String run(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try (var br = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            if (p.waitFor()!=0) throw new RuntimeException("non-zero exit");
            return sb.toString();
        }
    }

    /* ---------- 마스터 M3U8 파서 (#EXT-X-STREAM-INF) ---------- */
    // 파싱후 서브 메니페스트의 url을 저장한다.

    private static final Pattern STREAM_INF_ATTR =
            Pattern.compile("(\\w+)=((?:\"[^\"]+\")|[^,]+)");

    private static List<VariantDTO> parseMasterM3u8(String masterUrl) {
        try {
            HttpClient hc = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(masterUrl)).GET().build();
            String body = hc.send(req, HttpResponse.BodyHandlers.ofString()).body();

            List<String> lines = body.lines().toList();
            List<VariantDTO> out = new ArrayList<>();
            URI base = URI.create(masterUrl);

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
                String uri = null;
                int j = i + 1;
                while (j < lines.size()) {
                    String nxt = lines.get(j).trim();
                    if (!nxt.isEmpty() && !nxt.startsWith("#")) { uri = nxt; break; }
                    j++;
                }
                if (uri == null) continue;

                String resolution = kv.getOrDefault("RESOLUTION", null); // e.g. 1920x1080
                Integer bandwidth = null;
                if (kv.containsKey("BANDWIDTH")) {
                    try { bandwidth = Integer.valueOf(kv.get("BANDWIDTH")); } catch (NumberFormatException ignore) {}
                }
                String audioGroup = kv.getOrDefault("AUDIO", null);
                String absUri = base.resolve(uri).toString();

                out.add(new VariantDTO(resolution, bandwidth, absUri, audioGroup));
            }
            return out;
        } catch (Exception e) {
            // 네트워크 실패해도 probe 자체는 진행 가능하도록 빈 리스트 반환
            return List.of();
        }
    }

}
