package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter.ValidateCheckService;
import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;


@Service
@Slf4j
@RequiredArgsConstructor
public class ValidateCheckServiceImpl implements ValidateCheckService {

    private final RedisMediaService redis;

    @Override
    public ValidationResult checkValidation(CheckValidDTO dto) {
        // redis에 있는 history 호출
        List<CheckValidDTO> dtos = redis.getHistory(dto.traceId(), dto.resolution());


        ValidationResult r;

        // 이전 기록이 있다면 실행
        try {
            if (!dtos.isEmpty()) {
                r = checkChunkList(dtos, dto);
                if (r != ValidationResult.OK_FINE) return r;

                r = invalidSequenceChange(dtos, dto);
                if (r != ValidationResult.OK_FINE) return r;
            }
            r = ValidationResult.OK_FINE;
        } finally {
            redis.pushHistory(dto.traceId(), dto.resolution(), dto, 10);
        }

        return r;
    }


    // 1. 미디어 시퀀스는 변했으나 chunk의 목록이 바뀌지 않은경우
    public ValidationResult checkChunkList(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {
        // 바로 이전의 m3u8
        CheckValidDTO prev = previousDTOs.getFirst();

        // seq 증가가 아니면 이 룰의 대상이 아님 (동일/역행은 다른 룰에서 처리)
        if (curr.seq() <= prev.seq()) return ValidationResult.OK_FINE;

        // "m3u8 내용물이"이 동일한지 보수적으로 확인
        boolean segmentsIdentical =
                prev.segmentCount() == curr.segmentCount() &&
                        safeEq(prev.segFirstUri(), curr.segFirstUri()) &&
                        safeEq(prev.segLastUri(), curr.segLastUri()) &&
                        listEq(prev.tailUris(), curr.tailUris());

        if (segmentsIdentical) {
            return ValidationResult.WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL;
        }
        return ValidationResult.OK_FINE;
    }

//2. 미디어 시퀀스가 1이 아니게 변한경우
    public ValidationResult invalidSequenceChange(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {
        // 바로 이전의 m3u8
        CheckValidDTO prev = previousDTOs.getFirst();
        long seqDifference = curr.seq() - prev.seq();
        // 정상의 경우
        if(seqDifference == 1) return ValidationResult.OK_FINE;

        //2.1 같은 경우
        // -> 3회 이상 확인하여 에러 생성, seg가 변경되었다면 warn
        if(seqDifference == 0){
            // seg 변경 확인
            if(!curr.segLastUri().equals(prev.segLastUri())){
                return ValidationResult.WARN_SEGMENTS_CHANGED_SEQ_STUCK;
            }




            int sameCount = 1; // 해당 포함
            long currSeq = curr.seq();
            // 이전것들이 같은지 확인
            for (CheckValidDTO dto : previousDTOs) {
                if (dto.seq() == currSeq) {
                    sameCount++;
                } else {
                    // 연속 구간만 보면 되므로 다른 값 나오면 중단
                    break;
                }
            }

            if(sameCount >= 3){
                return ValidationResult.ERROR_STALL_NO_PROGRESS;
            }
        }


        // 2.2 5이상 늘어난 경우
        if(seqDifference >= 5){
            return ValidationResult.WARN_SEQUENCE_CHANGE_TOO_FAR;
        }
        //2.3 2 이상 늘어난경우
        // -> 인터넷에 의해 파일이 밀렸을 수 있으므로 청크가 새로 입력된 경우 문제가 아니라고 판단

        if (seqDifference >= 2) {
            Long prevFirst = num(prev.segFirstUri());
            Long currFirst = num(curr.segFirstUri());
            Long prevLast = num(prev.segLastUri());
            Long currLast = num(curr.segLastUri());
            boolean canCheckNumber =
                    prevFirst != null && currFirst != null
                            && prevLast != null && currLast != null;

            // 이전 데이터가 존재해야 확인이 가능하다.
            if (canCheckNumber) {
                long firstDiff = currFirst - prevFirst;
                long lastDiff = currLast - prevLast;

                // seq 증가량과 segment 번호 증가량이 정확히 맞으면
                // "파일이 밀렸지만 연속성은 유지"로 판단 (정보 레벨 정도)
                if (firstDiff == seqDifference && lastDiff == seqDifference) {
                    return ValidationResult.OK_FINE;
                }
                else{

                    // discontinuity 가 존재한다면 연속성이 유지되지 않을 수 있음
                    if(!curr.discontinuityPos().isEmpty()){
                        return ValidationResult.OK_FINE;
                    }
                    return ValidationResult.ERROR_MEDIA_SEQUENCE_SEGMENT_MISMATCH;
                }
            }
        }

        //2.2 줄어든 경우
        // 심각한 에러
        if(seqDifference < 0){
            return ValidationResult.ERROR_SEQ_REWIND;
        }

        return ValidationResult.OK_FINE; // 여기까지 올 일 없습니다.
    }


    // 3. discontinuity 시퀀스가 #EXT-X-DISCONTINUITY 가 사라졌음에도 변하지 않은경우
    // 근데 이런경우가 너무 과하게 빈번해서 구현해야하나?
    // 필요시 구현
    public ValidationResult checkDiscontinuity(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {


        // 3.1 discontinuity sequence 가 역행한경우


        // 3.2 discontinuity sequence가 증가하지 않은경우


        // 3.3 discontinuity sequence가 그냥 증가한경우


        return null;
    }



    // 4.이전 .m3u8과 청크의 갯수가 크게 차이나는경우
    // 광고를 어떻게 처리해야할지 애매~
    public ValidationResult checkChunkCount() {
        return null;
    }

    private static boolean safeEq(String a, String b) {
        return Objects.equals(a, b);
    }

    private static boolean listEq(List<String> a, List<String> b) {
        if (a == null || a.isEmpty()) return b == null || b.isEmpty();
        if (b == null || b.isEmpty()) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private static Long num(String s) {
        final Pattern NUM = Pattern.compile("(\\d+)(?:\\.[^.]+)?$");
        var m = NUM.matcher(s);
        if (!m.find()) return null;
        try { return Long.parseLong(m.group(1)); }
        catch (NumberFormatException e) { return null; }
    }
}
