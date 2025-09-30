package AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter;

import AutoMonitoring.AutoMonitoring.domain.ffmpeg.dto.ProbeCommand;
import AutoMonitoring.AutoMonitoring.domain.program.dto.ProbeDTO;

public interface MediaProbe {
    ProbeDTO probe(ProbeCommand dto);
}
