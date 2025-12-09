package AutoMonitoring.AutoMonitoring.contract.checkMediaValid;

import AutoMonitoring.AutoMonitoring.contract.Command;
import lombok.Builder;

@Builder
public record UpdateAlarmConfigCommand(
        boolean alarmEnabled,
        int threshold,
        int alarmCooldownSeconds
) implements Command {
}
