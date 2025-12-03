package AutoMonitoring.AutoMonitoring.contract.ffmpeg;

public record ProbeCommand(String traceId, String masterUrl, String userAgent)
implements FfmpegCommand{
}
