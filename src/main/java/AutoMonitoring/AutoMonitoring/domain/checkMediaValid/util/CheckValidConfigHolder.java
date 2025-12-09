package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.util;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.UpdateAlarmConfigCommand;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Getter
public class CheckValidConfigHolder {

    private final AtomicBoolean alarmEnabled;
    private final AtomicInteger threshold;
    private final AtomicInteger alarmCooldownSeconds;

    public CheckValidConfigHolder() {
        this.alarmEnabled = new AtomicBoolean(true);
        this.threshold = new AtomicInteger(5);
        this.alarmCooldownSeconds = new AtomicInteger(3600);
    }

    public void updateConfig(UpdateAlarmConfigCommand command) {
        this.alarmEnabled.set(command.alarmEnabled());
        this.threshold.set(command.threshold());
        this.alarmCooldownSeconds.set(command.alarmCooldownSeconds());
    }
}
