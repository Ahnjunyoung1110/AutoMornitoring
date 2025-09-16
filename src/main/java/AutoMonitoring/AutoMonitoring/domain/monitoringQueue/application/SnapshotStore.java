package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnapshotStore {

    private final RedisService redis;

    // traceId_resolution_UTCyyyyMMdd_HHmmss.exp<epochMs>.m3u8
    public Path saveSnapshot(Path baseDir,
                                    String traceId,
                                    String resolution,
                                    String mediaSequence,
                                    String content) throws IOException {

        Files.createDirectories(baseDir);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC).format(Instant.now());

        Duration ttl = Duration.ofHours(5L);

        // 외부 광고를 저장하는 옵션이 켜져 있는 경우에만 20일 동안 저장
        if(redis.getValues(RedisKeys.argument_record_discontinuity(traceId)).startsWith("true")){
            // #EXT-X-DISCONTINUITY 를 찾고 그 다음에 나오는 ts 요청이 Adslate인지 아닌지 판단하는 함수
            String[] lines = content.split("\n"); // 이미 \r\n -> \n 정규화했다고 가정
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (!line.startsWith("#EXT-X-DISCONTINUITY")) continue;

                if (i + 1 < lines.length) {
                    String nextEXTINF = lines[i + 1].trim();
                    String nextTS = lines[i + 2].trim();

                    // http로 시작하는 경우 ssai임. 예) ../../../../경로~
                    if (!nextTS.startsWith("http*")){
                        ttl = Duration.ofDays(7L);
                    }
                }
                break;
            }
        }





        long expiresEpochSec  = Instant.now().plus(ttl).getEpochSecond();
        // 파일 이름 지정
        String leaf = "%s_%s.exp%d.m3u8".formatted(timestamp, mediaSequence,expiresEpochSec);

        // 최종 경로: baseDir/traceId/resolution/leaf
        Path finalPath = baseDir.resolve(Paths.get(traceId, resolution, leaf));
        Path temp      = finalPath.resolveSibling(finalPath.getFileName() + ".part");



        // 부모 디렉터리 보장
        Files.createDirectories(finalPath.getParent());


        // .part 로 쓰고 ATOMIC_MOVE 로 교체 (부분 쓰기 노출 방지)
        Files.writeString(temp, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(temp, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        return finalPath;
    }
}
