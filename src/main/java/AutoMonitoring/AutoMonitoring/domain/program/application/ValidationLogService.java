package AutoMonitoring.AutoMonitoring.domain.program.application;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ValidationLog;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ValidationLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * M3U8 유효성 검사 실패 이력을 저장하는 서비스
 */
@Service
@RequiredArgsConstructor
public class ValidationLogService {

    private final ValidationLogRepo validationLogRepo;

    @Transactional
    public void saveValidationFailure(Program program, CheckValidDTO dto, String reason) {
        // List<Integer> to String
        String discontinuityPosStr = Optional.ofNullable(dto.discontinuityPos())
                .map(list -> list.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .orElse(null);

        // List<String> to String
        String tailUrisStr = Optional.ofNullable(dto.tailUris())
                .map(list -> String.join(",", list))
                .orElse(null);

        ValidationLog validationLog = ValidationLog.builder()
                .program(program)
                .validatedAt(Instant.now())
                .reason(reason)
                .resolution(dto.resolution())
                .tsEpochMs(dto.tsEpochMs())
                .requestDurationMs(dto.requestDurationMs() != null ? dto.requestDurationMs().toMillis() : null)
                .seq(dto.seq())
                .dseq(dto.dseq())
                .discontinuityPos(discontinuityPosStr)
                .segmentCount(dto.segmentCount())
                .hashNorm(dto.hashNorm())
                .segFirstUri(dto.segFirstUri())
                .segLastUri(dto.segLastUri())
                .tailUris(tailUrisStr)
                .wrongExtinf(dto.wrongExtinf())
                .build();

        validationLogRepo.save(validationLog);
    }
}
