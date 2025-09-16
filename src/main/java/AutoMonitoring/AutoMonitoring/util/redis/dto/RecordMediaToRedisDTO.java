package AutoMonitoring.AutoMonitoring.util.redis.dto;

import java.time.Duration;
import java.time.Instant;
public record RecordMediaToRedisDTO(
        Instant tsEpochMs,  // 수집시각
        Duration requestDurationMs,
        long seq,           // media-sequence의 값
        long dseq,          // #discontinuity sequence의 값
        int disCount,  // 해당 m3u8에 #EXT-X-discontinuity 가 얼마나 있는가
        int segmentCount, // 몇개의 청크가 입력되어있는가
        String hashNorm,      // 설정이 바뀌지는 않았는지를 확인하기 위한 정규화 후 hash값
        String segFirstUri,    // 첫 세그먼트 URI (쿼리X)
        String segLastUri,       // 마지막 세그먼트 URI (쿼리X)
        String tailUrisJson, // ← JSON ["seg123.ts","seg124.ts","seg125.ts"]의 마지막 3개
        boolean wrongExtinf // EXTINF 가 5가 아닌데 이후에 #EXT-X-DISCONTINUITY 가 등장하지 않은경우 true
)

{

}
