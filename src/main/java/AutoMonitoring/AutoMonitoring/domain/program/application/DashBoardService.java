package AutoMonitoring.AutoMonitoring.domain.program.application;

import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import AutoMonitoring.AutoMonitoring.domain.api.dto.DashBoardSummaryDTO;
import AutoMonitoring.AutoMonitoring.domain.api.dto.MonitoringProgramDTO;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashBoardService {
    private final ProgramRepo repo;

    public DashBoardSummaryDTO summary(){
//        int countAllPrograms = repo
        return null;
    }


    @Transactional(readOnly = true)
    public Page<MonitoringProgramDTO> getMonitoringPrograms(ResolutionStatus resolutionStatus, String traceId, String channelId, String tp, Pageable pageable) {
        Page<Program> programPage = repo.findMonitoring(resolutionStatus, traceId, channelId, tp, pageable);
        return programPage.map(program -> MonitoringProgramDTO.builder()
                .traceId(program.getTraceId())
                .masterManifestUrl(program.getMasterManifestUrl())
                .channelName(program.getChannelName())
                .channelId(program.getChannelId())
                .tp(program.getTp())
                .build());
    }
}
