package AutoMonitoring.AutoMonitoring.util.path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.storage")
public record StorageProps(
        String m3u8Dir,
        String tsDir
) {
}
