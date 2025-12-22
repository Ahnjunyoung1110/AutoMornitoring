package AutoMonitoring.AutoMonitoring.contract.program;


public record VariantDTO(
        String resolution,  // "1920x1080" (선택)
        Integer bandwidth,  // BANDWIDTH (bps, 선택)
        String uri,         // 서브 메니페스트 절대/상대→절대
        String audioGroup   // HLS AUDIO group-id (선택)
) {}
