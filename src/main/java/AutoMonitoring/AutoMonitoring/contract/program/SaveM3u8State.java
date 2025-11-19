package AutoMonitoring.AutoMonitoring.contract.program;

// .m3u8 저장 옵션
public enum SaveM3u8State {
    NONE,               // 저장안함
    WITHOUT_ADSLATE,    // 에드 슬레이트를 제외한 광고 나올시 저장
    WITH_ADSLATE,       // 에드 슬레이트를 포함한 광고 나올시 저장
    ALWAYS              // discontinuity 와 관계없이 모든 경우 저장
}
