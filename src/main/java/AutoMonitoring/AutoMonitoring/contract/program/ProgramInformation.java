package AutoMonitoring.AutoMonitoring.contract.program;

import java.util.List;

public record ProgramInformation(
        ResolutionStatus status,
        String mediaUrl,
        List<String> allUrls,
        Integer bandwidth,
        List<Integer> allBandwidths
) {
    // 기존 코드 호환용 생성자 (2-arg)
    public ProgramInformation(ResolutionStatus status, String mediaUrl) {
        this(status, mediaUrl, List.of(mediaUrl), null, List.of());
    }

    // 새로운 생성자 (List 버전)
    public ProgramInformation(ResolutionStatus status, List<String> urls, List<Integer> bandwidths) {
        this(
                status,
                urls.isEmpty() ? null : urls.get(0),
                urls,
                bandwidths.isEmpty() ? null : bandwidths.get(0),
                bandwidths
        );
    }
}
