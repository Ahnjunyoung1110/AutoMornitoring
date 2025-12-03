package AutoMonitoring.AutoMonitoring.contract.ffmpeg;

public record RefreshCommand (String traceId, String masterUrl, String userAgent)
        implements FfmpegCommand{
}