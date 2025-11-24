package AutoMonitoring.AutoMonitoring.domain.api.service;


import AutoMonitoring.AutoMonitoring.domain.api.dto.DashBoardSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("apiDashBoardService")
@Slf4j
@RequiredArgsConstructor
public class DashBoardService {

    // 대시보드에 출력될 요약을 가져오는 함수
    public DashBoardSummaryDTO summary(){
        return null;
    }
}
