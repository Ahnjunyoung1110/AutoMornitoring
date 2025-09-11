package AutoMonitoring.AutoMonitoring.domain.program.adapter;

import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramAlreadyExistException;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProgramServiceTest {

    ProgramService programService;
    @Autowired
    ProgramRepo programRepo;
    @Autowired
    ProgramServiceTest (ProgramService programService){
        this.programService = programService;
    }

    Program testProgram = ProgramBuilderSample.sample();


    @BeforeEach
    void setUp(){
        programRepo.deleteAll();
    }
    @Test
    void saveProgramO() {
        Program saved = programService.saveProgram(testProgram);
        Assertions.assertThat(saved.getId()).isNotNull();
        Assertions.assertThat(saved.getTraceId()).isEqualTo("TRACE-1234567890");
        Assertions.assertThat(saved.getStreams()).hasSize(2);
        Assertions.assertThat(saved.getVariants()).hasSize(2);
    }

    @Test
    void saveProgramX(){
        programService.saveProgram(testProgram);
        Program dup = ProgramBuilderSample.sample();

        Assertions.assertThatThrownBy(() -> programService.saveProgram(dup)).isInstanceOf(ProgramAlreadyExistException.class);

    }
}