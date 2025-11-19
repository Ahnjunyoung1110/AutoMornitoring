package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter.ValidateCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;


@Service
@Slf4j
public class ValidateCheckServiceImpl implements ValidateCheckService {
    @Override
    public ValidationResult checkValidation(CheckValidDTO dto) {

        return null;
    }
    // redis

    // 1. 미디어 시퀀스는 변했으나 chunk의 목록이 바뀌지 않은경우
    public Optional<ValidationResult> checkChunkList(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {
        // 바로 이전의 m3u8
        CheckValidDTO prev = previousDTOs.getFirst();

        // seq 증가가 아니면 이 룰의 대상이 아님 (동일/역행은 다른 룰에서 처리)
        if (curr.seq() <= prev.seq()) return Optional.empty();

        // "m3u8 내용물이"이 동일한지 보수적으로 확인
        boolean segmentsIdentical =
                prev.segmentCount() == curr.segmentCount() &&
                        safeEq(prev.segFirstUri(), curr.segFirstUri()) &&
                        safeEq(prev.segLastUri(), curr.segLastUri()) &&
                        listEq(prev.tailUris(), curr.tailUris());

        if (segmentsIdentical) {
            return Optional.of(ValidationResult.WARN_SEQ_ROLLED_SEGMENTS_IDENTICAL);
        }
        return Optional.empty();
    }


    //
//2. 미디어 시퀀스가 1이 아니게 변한경우
    public Optional<ValidationResult> invalidSequenceChange(List<CheckValidDTO> previousDTOs, CheckValidDTO curr) {
        // 바로 이전의 m3u8
        CheckValidDTO prev = previousDTOs.getFirst();
        long seqDifference = curr.seq() - prev.seq();

        // 2.1 5이상 늘어난 경우
        if(seqDifference >= 5){
            return Optional.of(ValidationResult.WARN_SEQUENCE_CHANGE_TOO_FAR);
        }
        //2.1 2 이상 늘어난경우
        // -> 인터넷에 의해 파일이 밀렸을 수 있으므로 청크가 새로 입력된 경우 문제가 아니라고 판단

        if (seqDifference >= 2) {
            Long prevFirst = num(prev.segFirstUri());
            Long currFirst = num(curr.segFirstUri());
            Long prevLast = num(prev.segLastUri());
            Long currLast = num(curr.segLastUri());
            boolean canCheckNumber =
                    prevFirst != null && currFirst != null
                            && prevLast != null && currLast != null;

            if (canCheckNumber) {
                long firstDiff = currFirst - prevFirst;
                long lastDiff = currLast - prevLast;

                // seq 증가량과 segment 번호 증가량이 정확히 맞으면
                // "파일이 밀렸지만 연속성은 유지"로 판단 (정보 레벨 정도)
                if (firstDiff == seqDifference && lastDiff == seqDifference) {
                    return Optional.of(ValidationResult.OK_FINE);
                }
                else{
                    return Optional.of(ValidationResult.ERROR_MEDIA_SEQUENCE_SEGMENT_MISMATCH);
                }
            }
        }

        //2.2 줄어든 경우
// -> 여러번 지속되는 경우 알람 생성

//2.3 같은 경우
// -> 3회 이상 확인하여 같다면 알람 생성
//
        return null;
    }


//            3. discontinuity 시퀀스가 #EXT-X-DISCONTINUITY 가 사라졌음에도 변하지 않은경우


    public ValidationResult checkDIscontinuity() {
        return null;
    }

    //    4.이전 .m3u8과 청크의 갯수가 크게 차이나는경우
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

    private static boolean safeUrlDifferenceCount(String prevFirstUrl, String nowFirstUrl, long howManyDifference, boolean discontinuity) {
        if (prevFirstUrl == null || nowFirstUrl == null) return false;



        if (discontinuity) return true; // discontinuity 가 존재하는 경우 ts 순번이 크게 바뀔 수 있음

        return false;
    }

    private static Long num(String s) {
        final Pattern NUM = Pattern.compile("(\\d+)(?:\\.[^.]+)?$");
        var m = NUM.matcher(s);
        if (!m.find()) return null;
        try { return Long.parseLong(m.group(1)); }
        catch (NumberFormatException e) { return null; }
    }
}
