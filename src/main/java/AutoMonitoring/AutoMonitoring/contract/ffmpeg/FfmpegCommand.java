package AutoMonitoring.AutoMonitoring.contract.ffmpeg;

public sealed interface FfmpegCommand
        permits ProbeCommand, RefreshCommand {
    String traceId();
    String masterUrl();
    String userAgent();
}
