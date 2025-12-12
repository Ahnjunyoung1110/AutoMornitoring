package AutoMonitoring.AutoMonitoring.domain.program.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SystemConfig {

    @Id
    private Long id;

    // 알람 전송 미전송 설정
    private boolean alarmEnabled = true;

    // 알람 발송 쓰레시홀드 설정 (0 이하는 의미 없음 → 0이면 default 강제)
    private int threshold;

    // 같은 채널/원인에 대해 알람 재발송 최소 간격(초)
    private int alarmCooldownSeconds;

    // 최대 연결 재시도 횟수 설정
    private int reconnectThreshold;

    // 재시도시 timeout 시간 (ms)
    private int reconnectTimeoutMillis;

    // 재시도시 각 딜레이 시간 (ms)
    private int reconnectRetryDelayMillis;

    // http 요청 타임아웃 (ms)
    private int httpRequestTimeoutMillis;

    // 리프레쉬 설정(최종 재시도 실패시 리프레쉬 여부, 성공시 알람 x)
    private boolean autoRefresh = false;

    // 전체 모니터링 기능 on/off
    private boolean monitoringEnabled = true;

    @PrePersist
    private void prePersist() {
        // id 없으면 1번으로 고정
        if (id == null) {
            id = 1L;
        }

        // 아래 값들은 "0이면 아직 설정 안 된 것"으로 간주하고 디폴트 세팅
        if (threshold <= 0) {
            threshold = 5;
        }

        if (alarmCooldownSeconds <= 0) {
            alarmCooldownSeconds = 3600; // 예: 3600초
        }

        if (reconnectThreshold <= 0) {
            reconnectThreshold = 10;
        }

        if (reconnectTimeoutMillis <= 0) {
            reconnectTimeoutMillis = 5_000; // 5초
        }

        if (reconnectRetryDelayMillis <= 0) {
            reconnectRetryDelayMillis = 2_000; // 2초
        }

        if (httpRequestTimeoutMillis <= 0) {
            httpRequestTimeoutMillis = 5_000;
        }
    }
}
