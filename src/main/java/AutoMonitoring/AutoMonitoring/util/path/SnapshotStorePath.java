package AutoMonitoring.AutoMonitoring.util.path;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@RequiredArgsConstructor
@Service
public class SnapshotStorePath {
    private final StorageProps props;

    public Path m3u8Base() {return Path.of(props.m3u8Dir());}
    Path tsBase() { return Path.of(props.tsDir()); }
}
