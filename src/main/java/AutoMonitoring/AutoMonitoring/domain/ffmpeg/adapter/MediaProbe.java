package AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter;

import AutoMonitoring.AutoMonitoring.domain.program.dto.ProbeDTO;

public interface MediaProbe {
    ProbeDTO probe(String masterManifestUrl);
}
