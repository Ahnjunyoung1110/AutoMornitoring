package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.util;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.AlarmConfigCommand;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Getter
public class AlarmConfigHolder {
    private final AtomicBoolean alarmEnabled;
    private final AtomicInteger threshold;
    private final AtomicInteger alarmCooldownSeconds;

    public AlarmConfigHolder(){
        this.alarmEnabled = new AtomicBoolean(true);
        this.threshold = new AtomicInteger(5);
        this.alarmCooldownSeconds = new AtomicInteger(60);
    }

    public void updateConfig(AlarmConfigCommand command){
        this.alarmEnabled.set(command.alarmEnabled());
        this.threshold.set(command.threshold());
        this.alarmCooldownSeconds.set(command.alarmCooldownSeconds());
    }
}
