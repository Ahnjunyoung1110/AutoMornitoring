package AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter;

import AutoMonitoring.AutoMonitoring.contract.ffmpeg.FfmpegCommand;
import AutoMonitoring.AutoMonitoring.contract.program.ProbeDTO;

public interface MediaProbe {
    ProbeDTO probe(FfmpegCommand dto);
}
