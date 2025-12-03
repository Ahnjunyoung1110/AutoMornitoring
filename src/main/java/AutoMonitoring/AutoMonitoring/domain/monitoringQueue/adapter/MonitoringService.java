package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.StopMonitoringMQCommand;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;

public interface MonitoringService {
    void startMornitoring(StartMonitoringDTO startMonitoringDTO);

    void stopMornitoring(StopMonitoringMQCommand stopMonitoringMQCommand);
}
