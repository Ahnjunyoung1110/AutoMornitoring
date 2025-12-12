package AutoMonitoring.AutoMonitoring.contract.checkMediaValid;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record CheckValidDTO(
        String traceId,
        String resolution,
        Instant tsEpochMs,  // 수집시각
        Duration requestDurationMs,
        long seq,           // media-sequence의 값
        long dseq,          // #discontinuity sequence의 값
        List<Integer> discontinuityPos,  // 해당 m3u8에 #EXT-X-discontinuity 가 몇번쨰 uri 앞에 있는가
        int segmentCount, // 몇개의 청크가 입력되어있는가
        String hashNorm,      // 설정이 바뀌지는 않았는지를 확인하기 위한 정규화 후 hash값
        String segFirstUri,    // 첫 세그먼트 URI (쿼리X)
        String segLastUri,       // 마지막 세그먼트 URI (쿼리X)
        List<String> tailUris, // ← ["seg123.ts","seg124.ts","seg125.ts"]의 마지막 3개
        boolean wrongExtinf, // EXTINF 가 5가 아닌데 이후에 #EXT-X-DISCONTINUITY 가 등장하지 않은경우 true
        boolean refresh     // true 인 경우 기존의 데이터를 모두 정리하고 새로 시작한다.(오류 발생 X)
) {
}
