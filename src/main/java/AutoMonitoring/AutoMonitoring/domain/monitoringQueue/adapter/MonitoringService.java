package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.domain.dto.StartMonitoringDTO;
import AutoMonitoring.AutoMonitoring.domain.dto.StopMornitoringDTO;

public interface MonitoringService {
    void startMornitoring(StartMonitoringDTO dto);
    void stopMornitoring(StopMornitoringDTO stopMornitoringDTO);
}
