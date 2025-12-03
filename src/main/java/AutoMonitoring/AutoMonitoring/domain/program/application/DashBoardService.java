package AutoMonitoring.AutoMonitoring.domain.program.application;

import AutoMonitoring.AutoMonitoring.domain.api.dto.DashBoardSummaryDTO;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashBoardService {
    private final ProgramRepo repo;

    public DashBoardSummaryDTO summary(){
//        int countAllPrograms = repo
        return null;
    }
}
