package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter.ParseMediaManifest;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParseMediaManifestImpl implements ParseMediaManifest {
    @Override
    public RecordMediaToRedisDTO parse(String manifest) {
        if (manifest == null) throw new IllegalArgumentException("파싱하려는 매니페스트가 존재하지 않습니다.");

        String m = manifest.replace("\r\n", "\n").trim();

        // 헤더 파싱
        long seq = findLong(m,"^#EXT-X-MEDIA-SEQUENCE:(\\d+)$", -1L);
        long dseq = findLong(m, "^#EXT-X-DISCONTINUITY-SEQUENCE:(\\d+)$", -1L);
        int targetDuration = (int) findLong(m, "^#EXT-X-TARGETDURATION:(\\d+)$", -1L);

        // 청크 파싱
        List<String> uris = new ArrayList<>();
        int disCount = 0;
        boolean wrongExtinf = false;

        final double eps = 0.05; // 이정도의 오차는 DISCONTINUITY 가 없어도 혀용하겠어요!
        Pattern extinfPat = Pattern.compile("^#EXTINF:([0-9]+(?:\\.[0-9]+)?),\\s*$");
        boolean pendingExtinfOff = false; // 직전 EXTINF가 target과 유의미하게 다른가?


        for (String line : m.split("\n")){
            String s = line.trim();
            if(s.isEmpty()) continue;

            // #EXTINF 가 입력된 줄일 경우
            if(s.startsWith("#EXTINF:")){
                Matcher em = extinfPat.matcher(s);
                if (em.find()){
                    double dur = Double.parseDouble(em.group(1));
                    pendingExtinfOff = Math.abs(dur - targetDuration) > eps;
                }
                continue;
            }

            // EXT-X-DISCONTINUITY 가 있는경우
            if(s.equals("#EXT-X-DISCONTINUITY")){
                disCount++;

                // 정상적인 EXTINF 라는 의미임으로 flase
                pendingExtinfOff = false;
                continue;
            }

            if (s.startsWith("#")) {
                // 다른 태그는 무시
                continue;
            }

            // 청크 uri 라인
            String normUri = stripQuery(s);
            uris.add(normUri);

            // 이전 EXTINF 가 이상했는데 EXT-X-DISCONTINUITY가 등장하지 않은경우
            if (pendingExtinfOff){
                wrongExtinf = true;
                pendingExtinfOff = false;
            }
        }

        int segmentCount = uris.size();
        String segFirstUri = segmentCount > 0 ? uris.get(0) : "";
        String segLastUri  = segmentCount > 0 ? uris.get(segmentCount - 1) : "";

        // tail 3개 JSON
        int size = uris.size();
        int from = Math.max(0, size - 3);
        List<String> tailNames = new ArrayList<>(
                uris.subList(from, size).stream()
                        .map(u -> extractTsName(stripQuery(u)))  // ← 여기서 파일명만
                        .toList()
        );
        String tailUrisJson = toJsonArray(tailNames);

        // 3) hashNorm 계산 (정규화: 일부 태그 제거 + URI 쿼리 제거)
        String normalized = m.lines()
                .map(String::trim)
                .filter(s -> !s.startsWith("#EXT-X-MEDIA-SEQUENCE"))
                .filter(s -> !s.startsWith("#EXT-X-PROGRAM-DATE-TIME"))
                .filter(s -> !s.startsWith("#EXT-X-DISCONTINUITY-SEQUENCE"))
                .map(s -> s.startsWith("#") ? s : stripQuery(s))
                .reduce(new StringBuilder(), (sb, s) -> (sb.length() > 0 ? sb.append('\n') : sb).append(s), StringBuilder::append)
                .toString();
        String hashNorm = sha256Hex(normalized.getBytes(StandardCharsets.UTF_8));

        // 4) DTO 생성
        return new RecordMediaToRedisDTO(
                Instant.now(),     // tsEpochMs
                seq,
                dseq,
                disCount,
                segmentCount,
                hashNorm,
                segFirstUri,
                segLastUri,
                tailUrisJson,
                wrongExtinf
        );
    }


// ---- helpers ----

    private static long findLong(String text, String regex, long def) {
        Matcher m = Pattern.compile(regex, Pattern.MULTILINE).matcher(text);
        return m.find() ? Long.parseLong(m.group(1)) : def;
    }

    private static String stripQuery(String uri) {
        int q = uri.indexOf('?');
        return q >= 0 ? uri.substring(0, q) : uri;
    }

    private static String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escapeJson(list.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        // 최소 escaping (따옴표/역슬래시)
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(data);
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // url 제일 마지막의 ts파일 명만 추출
    private static String extractTsName(String url) {
        String path = java.net.URI.create(url).getPath();      // 쿼리/프래그먼트 제외된 경로
        int slash = path.lastIndexOf('/');
        return (slash >= 0) ? path.substring(slash + 1) : path; // 마지막 세그먼트 = 파일명
    }
}
