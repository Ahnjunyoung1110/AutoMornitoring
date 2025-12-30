package AutoMonitoring.AutoMonitoring.contract.checkMediaValid;


public enum ValidationResult {
    OK_FINE,                                    // 정상적인 상태
    WARN_SEQUENCE_CHANGE_TOO_FAR,
    WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL,         // seq는 증가했는데 창(세그먼트 집합)은 동일
    WARN_SEGMENTS_CHANGED_SEQ_STUCK,            // 세그먼트는 바뀌었는데 seq는 그대로
    WARN_DSEQ_STALE_AFTER_REMOVAL,              // DISCONTINUITY 사라졌는데 dseq가 안 바뀜
    WARN_MEDIA_SEQUENCE_SEGMENT_MISMATCH,      // 시퀀스의 변화와 세그먼트의 변화가 일치하지 않음

    ERROR_STALL_NO_PROGRESS,                    // (LIVE) 진행 없음 N회 누적
    ERROR_SEQ_REWIND,                           // seq가 역행(작아짐)
    ERROR_SEGMENT_GAP_OR_OVERLAP,               // 세그먼트 누락/중복(겹침) 탐지
    ERROR_DSEQ_REWIND                           // dseq 역행, 잘 모르겠음.

}
