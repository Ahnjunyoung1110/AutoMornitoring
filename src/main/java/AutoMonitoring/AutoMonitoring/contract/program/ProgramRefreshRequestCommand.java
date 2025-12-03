package AutoMonitoring.AutoMonitoring.contract.program;

import jakarta.validation.constraints.NotBlank;

public record ProgramRefreshRequestCommand(
        @NotBlank(message = "traceId는 필수입니다.")
        String traceId
)
        implements ProgramCommand
{
}
