package AutoMonitoring.AutoMonitoring.domain.program.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;


@AllArgsConstructor
@Getter
public class ProgramInfo {
    private Map<String, String> resolutionToUrl;  // resolution과 그에 해당하는 URL을 매핑
    private String userAgent;
    private String traceId;


    public static ProgramInfo getProgramInfo(Program program) {


        // 여러 개의 resolution과 그에 맞는 URL을 매핑하여 Map에 저장
        Map<String, String> resolutionToUrl = Program.getResolutionToUrlDomain(program); // 예: {"1080p": "url1", "720p": "url2"}

        // UserAgent를 가져옵니다
        String userAgent = program.getUserAgent(); // 예: "Mozilla/5.0"

        // ProgramInfo 객체 생성 후 반환
        return new ProgramInfo(resolutionToUrl, userAgent, program.getTraceId());
    }
}

