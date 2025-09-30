package AutoMonitoring.AutoMonitoring.util.redis.application;

import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisMediaServiceImpl implements RedisMediaService {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    @Override
    public void saveState(String traceId, String resolution, RecordMediaToRedisDTO dto) {
        Map<String, String> m = toHash(dto);
        String key = RedisKeys.hashState(traceId,resolution);
        redis.opsForHash().putAll(key,m);
    }

    @Override
    public RecordMediaToRedisDTO getState(String traceId, String resolution) {
        String key = RedisKeys.hashState(traceId,resolution);
        Map<Object, Object> h = redis.opsForHash().entries(key);
        if (h == null || h.isEmpty()) return null;
        return fromHash(h);
    }

    @Override
    public void pushHistory(String traceId, String resolution, RecordMediaToRedisDTO dto, int max) {
        try {
            max = 10;
            String json = om.writeValueAsString(dto);
            String key = RedisKeys.hist(traceId, resolution);
            redis.opsForList().leftPush(key, json);
            redis.opsForList().trim(key, 0, Math.max(0, max - 1));
        } catch (Exception e) {
            throw new RuntimeException("pushHistory failed", e);
        }
    }




    // 내부 기능
    
    // dto를 HashMap 으로 변환
    private Map<String, String> toHash(RecordMediaToRedisDTO d) {
        Map<String, String> m = new HashMap<>();
        m.put("tsEpochMs", String.valueOf(d.tsEpochMs().toEpochMilli()));
        m.put("seq", String.valueOf(d.seq()));
        m.put("dseq", String.valueOf(d.dseq()));
        m.put("disCount", String.valueOf(d.disCount()));
        m.put("segmentCount", String.valueOf(d.segmentCount()));
        m.put("hashNorm", nullToEmpty(d.hashNorm()));
        m.put("segFirstUri", nullToEmpty(d.segFirstUri()));
        m.put("segLastUri", nullToEmpty(d.segLastUri()));
        m.put("tailUrisJson", nullToEmpty(d.tailUrisJson())); // 이미 JSON 문자열
        m.put("wrongExtinf", String.valueOf(d.wrongExtinf()));
        return m;
    }



    // Hashmap을 DTO 로 변환
    private RecordMediaToRedisDTO fromHash(Map<Object, Object> h) {
        Instant ts = Instant.ofEpochMilli(Long.parseLong(get(h, "tsEpochMs", "0")));
        long durMs = Long.parseLong(get(h, "getDurationMs", "0"));
        Duration getDurationMs = Duration.ofMillis(durMs);
        long seq = Long.parseLong(get(h, "seq", "0"));
        long dseq = Long.parseLong(get(h, "dseq", "0"));
        int disCount = Integer.parseInt(get(h, "disCount", "0"));
        int segmentCount = Integer.parseInt(get(h, "segmentCount", "0"));
        String hashNorm = get(h, "hashNorm", "");
        String segFirstUri = get(h, "segFirstUri", "");
        String segLastUri = get(h, "segLastUri", "");
        String tailUrisJson = get(h, "tailUrisJson", "[]");
        boolean wrongExtinf = Boolean.parseBoolean(get(h, "wrongExtinf", "false"));

        return new RecordMediaToRedisDTO(
                ts,getDurationMs , seq, dseq, disCount, segmentCount,
                hashNorm, segFirstUri, segLastUri, tailUrisJson, wrongExtinf
        );
    }

    private static String get(Map<Object, Object> h, String k, String def) {
        Object v = h.get(k);
        return v == null ? def : v.toString();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }




}
