package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter.ValidateCheckService;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.dto.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.vo.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
public class ValidateCheckServiceImpl implements ValidateCheckService {
    @Override
    public ValidationResult checkValidation(CheckValidDTO dto) {

        return null;
    }
    // redis

    // 1. 미디어 시퀀스는 변했으나 chunk의 목록이 바뀌지 않은경우
    public Optional<ValidationResult> checkChunkList(){


        return null;
    }


//
//2. 미디어 시퀀스가 1이 아니게 변한경우
    public ValidationResult invalidSequenceChange(){
        //2.1 2 이상 늘어난경우
        // -> 인터넷에 의해 파일이 밀렸을 수 있으므로 청크가 2개가 새로 입력된 경우 문제가 아니라고 판단
        //

        //2.2 줄어든 경우
// -> 여러번 지속되는 경우 알람 생성
//
//2.3 같은 경우
// -> 3회 이상 확인하여 같다면 알람 생성
//
        return null;
    }


//            3. discontinuity 시퀀스가 #EXT-X-DISCONTINUITY 가 사라졌음에도 변하지 않은경우


    public ValidationResult checkDIscontinuity(){
        return null;
    }
//    4.이전 .m3u8과 청크의 갯수가 크게 차이나는경우
    public ValidationResult checkChunkCount(){
        return null;
    }

}
