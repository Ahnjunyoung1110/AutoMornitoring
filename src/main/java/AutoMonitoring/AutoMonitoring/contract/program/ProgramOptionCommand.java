package AutoMonitoring.AutoMonitoring.contract.program;


import jakarta.validation.constraints.NotBlank;

public record ProgramOptionCommand(
        @NotBlank(message = "traceId는 필수입니다.")
        String traceId,
        SaveM3u8State saveM3u8State
) implements ProgramCommand {

}
