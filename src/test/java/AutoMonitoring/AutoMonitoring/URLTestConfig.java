package AutoMonitoring.AutoMonitoring;

public interface URLTestConfig {
    /**
     * 정상적인 HLS 미디어 매니페스트를 반환하는 공개된 테스트 URL
     */
    String SUCCESS_MANIFEST_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";

    /**
     * 존재하지 않아 실패를 유도하기 위한 테스트 URL
     */
    String INVALID_URL = "http://test.url/invalid/manifest.m3u8";
}
