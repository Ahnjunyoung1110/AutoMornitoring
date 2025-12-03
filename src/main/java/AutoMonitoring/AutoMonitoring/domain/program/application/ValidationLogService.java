package AutoMonitoring.AutoMonitoring.domain.program.application;

import AutoMonitoring.AutoMonitoring.contract.program.SaveFailureDTO;
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
    public void saveValidationFailure(Program program, SaveFailureDTO dto) {
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

                // --- 룰 정보 ---
                .ruleCode(dto.ruleCode())
                .detailReason(dto.detailReason())      // 사람이 읽기 위한 상세 설명

                // --- 현재 스냅샷 (CheckValidDTO 기반) ---
                .resolution(dto.resolution())
                .tsEpochMs(dto.tsEpochMs())
                .requestDurationMs(dto.requestDurationMs() != null
                        ? dto.requestDurationMs()
                        : null)
                .seq(dto.seq())
                .dseq(dto.dseq())
                .discontinuityPos(discontinuityPosStr)
                .segmentCount(dto.segmentCount())
                .hashNorm(dto.hashNorm())
                .segFirstUri(dto.segFirstUri())
                .segLastUri(dto.segLastUri())
                .tailUris(tailUrisStr)
                .wrongExtinf(dto.wrongExtinf())

                // --- 이전 윈도우 / 기대값 요약 ---
                .prevSeq(dto.prevSeq())                               // 없으면 null
                .prevLastSegmentSeq(dto.prevLastSegmentSeq())         // 없으면 null
                .currFirstSegmentSeq(dto.currFirstSegmentSeq())       // 이번 윈도우 첫 ts 번호
                .expectedFirstSegmentSeq(dto.expectedFirstSegmentSeq()) // 룰이 계산한 기대값, 없으면 null

                .build();

        validationLogRepo.save(validationLog);
    }
}
