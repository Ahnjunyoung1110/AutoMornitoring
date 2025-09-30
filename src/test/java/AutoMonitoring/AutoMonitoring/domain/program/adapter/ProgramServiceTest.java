package AutoMonitoring.AutoMonitoring.domain.program.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramAlreadyExistException;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgramServiceTest extends BaseTest {

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramRepo programRepo;

    @BeforeEach
    void setUp() {
        programRepo.deleteAll();
    }

    @Test
    @DisplayName("새로운 Program 엔티티를 성공적으로 저장한다.")
    void saveProgram_WhenProgramIsNew_ShouldSaveSuccessfully() {
        // given
        Program newProgram = ProgramBuilderSample.sample();

        // when
        Program saved = programService.saveProgram(newProgram);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTraceId()).isEqualTo("TRACE-1234567890");
        assertThat(saved.getStreams()).hasSize(2);
        assertThat(saved.getVariants()).hasSize(2);

        Program found = programRepo.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getTraceId()).isEqualTo(saved.getTraceId());
    }

    @Test
    @DisplayName("이미 존재하는 traceId의 Program을 저장하려고 하면 ProgramAlreadyExistException 예외가 발생한다.")
    void saveProgram_WhenTraceIdAlreadyExists_ShouldThrowException() {
        // given
        Program existingProgram = ProgramBuilderSample.sample();
        programService.saveProgram(existingProgram); // 먼저 하나 저장

        Program duplicateProgram = ProgramBuilderSample.sample(); // 같은 traceId를 가진 객체

        // when & then
        assertThatThrownBy(() -> programService.saveProgram(duplicateProgram))
                .isInstanceOf(ProgramAlreadyExistException.class);
    }
}