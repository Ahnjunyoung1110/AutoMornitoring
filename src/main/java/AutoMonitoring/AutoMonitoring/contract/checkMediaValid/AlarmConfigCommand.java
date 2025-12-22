package AutoMonitoring.AutoMonitoring.contract.checkMediaValid;

import AutoMonitoring.AutoMonitoring.contract.Command;

public record AlarmConfigCommand (
        boolean alarmEnabled,
        int threshold,
        int alarmCooldownSeconds
) implements Command {
}
