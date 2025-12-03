package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application;

import AutoMonitoring.AutoMonitoring.contract.program.SaveM3u8State;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public Path trySnapshot(Path baseDir,
                            String url,
                            String traceId,
                            String resolution,
                            String mediaSequence,
                            String content,
                            boolean isDiscontinuity

    ) throws IOException {

        SaveM3u8State state = loadState(traceId); // redis or default

        if (!shouldSnapshot(state, isDiscontinuity, url)) {
            return Path.of(""); // 아무것도 안 함
        }

        return saveSnapshot(baseDir, url, traceId, resolution, mediaSequence, content);
    }


    // redis 에서 m3u8 저장 옵션을 가져온다.
    private SaveM3u8State loadState(String traceId) {
        String saveOptionKey = RedisKeys.argument_record_discontinuity(traceId);
        String m3u8SaveOption = redis.getValues(saveOptionKey);
        try {
            return SaveM3u8State.valueOf(m3u8SaveOption);  // "NONE" -> SaveM3u8State.NONE
        } catch (IllegalArgumentException e) {
            // 이상한 값 들어온 경우 기본값 처리
            return SaveM3u8State.WITHOUT_ADSLATE;
        }
    }

    // 해당 m3u8이 저장해야 하는지 판단한다.
    private boolean shouldSnapshot(SaveM3u8State state,
                                   boolean isDiscontinuity,
                                   String url) {
        switch (state) {
            // 저장 안함
            case NONE -> {
                return false;
            }
            
            // 에드슬레이트 아닌경우만 저장
            case WITHOUT_ADSLATE-> {
                if (isDiscontinuity &&
                        !url.startsWith("https://cdn") && !url.startsWith("http://cdn") && !url.startsWith("https://cc-")){
                    return true;
                }
                else{
                    return false;
                }
            }
            
            // 에드슬레이트 포함해서 저장.
            case WITH_ADSLATE -> {
                return isDiscontinuity;
            }

            // discontinuity 와 관계없이 항상 저장
            case ALWAYS -> {
                // ALWAYS는 디스컨티뉴티 상관없이 허용
                // (url 기반 필터만 적용할지 여부는 여기서 결정)
                return true;
            }
        }

        // enum 모두 처리했으면 여기 안 옴
        return true;
    }

    // traceId_resolution_UTCyyyyMMdd_HHmmss.exp<epochMs>.m3u8
    public Path saveSnapshot(Path baseDir,
                                    String url,
                                    String traceId,
                                    String resolution,
                                    String mediaSequence,
                                    String content) throws IOException {


        // 저장 기간
        Duration ttl = Duration.ofHours(5L);

        Files.createDirectories(baseDir);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC).format(Instant.now());

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
