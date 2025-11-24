package AutoMonitoring.AutoMonitoring.contract.program;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;

/**
 * M3U8 유효성 검사 실패 시, 해당 정보를 DB에 기록하기 위해 발행되는 커맨드
 */
public record LogValidationFailureCommand(
        String traceId,
        String reason,
        CheckValidDTO validationData
) implements DbCommand {
}
