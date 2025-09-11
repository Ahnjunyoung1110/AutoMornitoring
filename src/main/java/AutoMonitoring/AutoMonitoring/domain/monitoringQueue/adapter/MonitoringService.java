package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.StopMornitoringDTO;

public interface MonitoringService {
    void startMornitoring(StartMonitoringDTO startMonitoringDTO);
    void stopMornitoring(StopMornitoringDTO stopMornitoringDTO);
}
