package AutoMonitoring.AutoMonitoring.domain.program.mqworker;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import AutoMonitoring.AutoMonitoring.contract.ffmpeg.RefreshCommand;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.SaveM3u8OptionCommand;
import AutoMonitoring.AutoMonitoring.contract.program.*;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ProgramInfo;
import AutoMonitoring.AutoMonitoring.domain.program.entity.ValidationLog;
import AutoMonitoring.AutoMonitoring.domain.program.entity.VariantInfoEmb;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramAlreadyExistException;
import AutoMonitoring.AutoMonitoring.domain.program.exception.ProgramNotFoundException;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ProgramRepo;
import AutoMonitoring.AutoMonitoring.domain.program.repository.ValidationLogRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DbWorkerTest extends BaseTest {

    @Autowired
    private DbWorker dbWorker;

    @Autowired
    private ProgramRepo programRepo;

    @Autowired
    private ValidationLogRepo validationLogRepo;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String traceId;
    private ProbeDTO probeDTO;

    @BeforeEach
    void setup() {
        validationLogRepo.deleteAll();
        programRepo.deleteAll();
        traceId = UUID.randomUUID().toString();
        probeDTO = createTestProbeDTO(traceId);
    }

    @Test
    @Transactional
    @DisplayName("handleDbCreateProbeCommand_Success: 새로운 프로그램 정보를 성공적으로 생성하고 다음 단계 메시지를 전송한다.")
    void handleDbCreateProbeCommand_Success() throws IOException {
        // given
        DbCreateProbeCommand command = new DbCreateProbeCommand(traceId, probeDTO);

        // when
        dbWorker.handle(command);

        // then
        Program savedProgram = programRepo.findByTraceId(traceId)
                .orElseThrow(() -> new AssertionError("Program not found in DB"));
        assertThat(savedProgram.getMasterManifestUrl()).isEqualTo(probeDTO.masterManifestUrl());
        assertThat(savedProgram.getFormat()).isEqualTo("hls");
        assertThat(savedProgram.getVariants()).hasSize(probeDTO.variants().size());

        Message receivedMessage = rabbitTemplate.receive(RabbitNames.Q_STAGE3, 2000);
        assertThat(receivedMessage).isNotNull();
        ProgramInfo receivedProgramInfo = objectMapper.readValue(new String(receivedMessage.getBody()), ProgramInfo.class);
        assertThat(receivedProgramInfo.getTraceId()).isEqualTo(traceId);
        assertThat(receivedProgramInfo.getUserAgent()).isEqualTo(probeDTO.userAgent());
        assertThat(receivedProgramInfo.getResolutionToUrl()).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("handleDbCreateProbeCommand_Fail_When_ProgramExists: 이미 존재하는 프로그램일 경우 예외를 발생시킨다.")
    void handleDbCreateProbeCommand_Fail_When_ProgramExists() {
        // given
        programRepo.save(Program.fromDto(probeDTO));
        DbCreateProbeCommand command = new DbCreateProbeCommand(traceId, probeDTO);

        // when & then
        assertThrows(ProgramAlreadyExistException.class, () -> dbWorker.handle(command));
    }

    @Test
    @Transactional
    @DisplayName("handleDbRefreshProbeCommand_Success: 기존 프로그램 정보를 갱신하고 다음 단계 메시지를 전송한다.")
    void handleDbRefreshProbeCommand_Success() throws IOException {
        // given
        programRepo.save(Program.fromDto(probeDTO));
        ProbeDTO refreshProbeDTO = ProbeDTO.builder()
                .traceId(traceId)
                .probeAt(Instant.now())
                .masterManifestUrl("http://example.com/refreshed_master.m3u8")
                .userAgent("RefreshedAgent")
                .format("hls")
                .durationSec(125.5)
                .overallBitrate(8000000)
                .saveM3u8State(SaveM3u8State.WITHOUT_ADSLATE)
                .streams(List.of(StreamDTO.builder().type("video").codec("h264").width(1280).height(720).build()))
                .variants(List.of(new VariantDTO("1280x720", 5000000, "http://example.com/refreshed_stream.m3u8", "audio_refreshed")))
                .build();
        DbRefreshProbeCommand command = new DbRefreshProbeCommand(traceId, refreshProbeDTO);

        // when
        dbWorker.handle(command);

        // then
        Program updatedProgram = programRepo.findByTraceId(traceId).get();
        assertThat(updatedProgram.getMasterManifestUrl()).isEqualTo("http://example.com/refreshed_master.m3u8");
        assertThat(updatedProgram.getDurationSec()).isEqualTo(125.5);
        assertThat(updatedProgram.getVariants()).hasSize(1);

        Message receivedMessage = rabbitTemplate.receive(RabbitNames.Q_STAGE3, 2000);
        assertThat(receivedMessage).isNotNull();
        ProgramInfo receivedProgramInfo = objectMapper.readValue(new String(receivedMessage.getBody()), ProgramInfo.class);
        assertThat(receivedProgramInfo.getTraceId()).isEqualTo(traceId);
        assertThat(receivedProgramInfo.getResolutionToUrl()).hasSize(1);
    }

    @Test
    @Transactional
    @DisplayName("handleDbRefreshProbeCommand_Fail_When_ProgramNotFound: 갱신할 프로그램이 없을 경우 예외를 발생시킨다.")
    void handleDbRefreshProbeCommand_Fail_When_ProgramNotFound() {
        // given
        DbRefreshProbeCommand command = new DbRefreshProbeCommand(traceId, probeDTO);
        // when & then
        assertThrows(ProgramNotFoundException.class, () -> dbWorker.handle(command));
    }

    @Test
    @Transactional
    @DisplayName("handleLogValidationFailureCommand_Success: 유효성 검사 실패 로그를 성공적으로 저장한다.")
    void handleLogValidationFailureCommand_Success() {
        // given
        programRepo.save(Program.fromDto(probeDTO));
        SaveFailureDTO failureDTO = createTestSaveFailureDTO(traceId);
        LogValidationFailureCommand command = new LogValidationFailureCommand(traceId, failureDTO);

        // when
        dbWorker.handle(command);

        // then
        assertThat(validationLogRepo.count()).isEqualTo(1);
        ValidationLog savedLog = validationLogRepo.findAll().get(0);
        assertThat(savedLog.getProgram().getTraceId()).isEqualTo(traceId);
        assertThat(savedLog.getRuleCode()).isEqualTo(failureDTO.ruleCode());
        assertThat(savedLog.getDetailReason()).isEqualTo(failureDTO.detailReason());
        assertThat(savedLog.getResolution()).isEqualTo(failureDTO.resolution());
        assertThat(savedLog.getSeq()).isEqualTo(failureDTO.seq());
        assertThat(savedLog.getPrevSeq()).isEqualTo(failureDTO.prevSeq());
    }

    @Test
    @Transactional
    @DisplayName("handleLogValidationFailureCommand_ProgramNotFound: 프로그램이 없을 경우 로그를 저장하지 않고 넘어간다.")
    void handleLogValidationFailureCommand_ProgramNotFound() {
        // given
        String nonExistentTraceId = "non-existent-trace-id";
        SaveFailureDTO failureDTO = createTestSaveFailureDTO(nonExistentTraceId);
        LogValidationFailureCommand command = new LogValidationFailureCommand(nonExistentTraceId, failureDTO);

        // when & then
        assertDoesNotThrow(() -> dbWorker.handle(command));
        assertThat(validationLogRepo.count()).isZero();
    }


    @Test
    @Transactional
    @DisplayName("handleDbGetStatusCommand_Success: 프로그램의 상태 정보를 성공적으로 조회한다.")
    void handleDbGetStatusCommand_Success() {
        // given
        Program program = Program.fromDto(probeDTO);
        program.findVariantByResolution("1920x1080").ifPresent(v -> v.changeStatus(ResolutionStatus.MONITORING));
        program.findVariantByResolution("1280x720").ifPresent(v -> v.changeStatus(ResolutionStatus.RETRYING));
        programRepo.save(program);
        DbGetStatusCommand command = new DbGetStatusCommand(traceId);

        // when
        Object result = dbWorker.handle(command);

        // then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, String> statusMap = (Map<String, String>) result;
        assertThat(statusMap.get("1920x1080")).isEqualTo(ResolutionStatus.MONITORING.name());
        assertThat(statusMap.get("1280x720")).isEqualTo(ResolutionStatus.RETRYING.name());
    }


    @Test
    @Transactional
    @DisplayName("handleProgramOptionCommand_Success: 프로그램의 저장 옵션을 변경하고 커맨드 메시지를 전송한다.")
    void handleProgramOptionCommand_Success() throws IOException {
        // given
        programRepo.save(Program.fromDto(probeDTO));
        ProgramOptionCommand command = new ProgramOptionCommand(traceId, SaveM3u8State.ALWAYS);

        // when
        dbWorker.handleCommand(command);

        // then
        Program updatedProgram = programRepo.findByTraceId(traceId).get();
        assertThat(updatedProgram.getSaveM3u8State()).isEqualTo(SaveM3u8State.ALWAYS);

        Message receivedMessage = rabbitTemplate.receive(RabbitNames.Q_MONITORING_COMMAND, 2000);
        assertThat(receivedMessage).isNotNull();
        SaveM3u8OptionCommand receivedCommand = objectMapper.readValue(new String(receivedMessage.getBody()), SaveM3u8OptionCommand.class);
        assertThat(receivedCommand.traceId()).isEqualTo(traceId);
        assertThat(receivedCommand.saveM3u8State()).isEqualTo(SaveM3u8State.ALWAYS);
    }

    @Test
    @Transactional
    @DisplayName("handleProgramRefreshRequestCommand_Success: 프로그램 갱신 요청 시, Probe 요청 메시지를 전송한다.")
    void handleProgramRefreshRequestCommand_Success() throws IOException {
        // given
        Program program = Program.fromDto(probeDTO);
        programRepo.save(program);
        ProgramRefreshRequestCommand command = new ProgramRefreshRequestCommand(traceId);

        // when
        dbWorker.handleCommand(command);

        // then
        Message receivedMessage = rabbitTemplate.receive(RabbitNames.Q_STAGE1, 2000);
        assertThat(receivedMessage).isNotNull();
        RefreshCommand receivedCommand = objectMapper.readValue(new String(receivedMessage.getBody()), RefreshCommand.class);
        assertThat(receivedCommand.traceId()).isEqualTo(traceId);
        assertThat(receivedCommand.masterUrl()).isEqualTo(program.getMasterManifestUrl());
    }

    @Test
    @Transactional
    @DisplayName("handleProgramRefreshRequestCommand_Fail_When_ProgramNotFound: 갱신 요청할 프로그램이 없을 경우 예외를 발생시킨다.")
    void handleProgramRefreshRequestCommand_Fail_When_ProgramNotFound() {
        // given
        ProgramRefreshRequestCommand command = new ProgramRefreshRequestCommand(traceId);
        // when & then
        assertThrows(ProgramNotFoundException.class, () -> dbWorker.handleCommand(command));
    }

    @Test
    @Transactional
    @DisplayName("handleProgramStatusCommand_Success: 프로그램의 특정 해상도 상태를 성공적으로 변경한다.")
    void handleProgramStatusCommand_Success() {
        // given
        Program program = Program.fromDto(probeDTO);
        programRepo.save(program);
        ProgramStatusCommand command = new ProgramStatusCommand(traceId, "1920x1080", ResolutionStatus.FAILED);

        // when
        dbWorker.handleCommand(command);

        // then
        Program updatedProgram = programRepo.findByTraceId(traceId).get();
        VariantInfoEmb updatedVariant = updatedProgram.findVariantByResolution("1920x1080").get();
        assertThat(updatedVariant.getStatus()).isEqualTo(ResolutionStatus.FAILED);
    }


    private ProbeDTO createTestProbeDTO(String traceId) {
        List<StreamDTO> streams = List.of(
                StreamDTO.builder().type("video").codec("h264").width(1920).height(1080).fps(29.97).build(),
                StreamDTO.builder().type("audio").codec("aac").channels(2).lang("eng").build()
        );

        List<VariantDTO> variants = List.of(
                new VariantDTO("1920x1080", 6000000, "http://example.com/stream1.m3u8", "audio_group1"),
                new VariantDTO("1280x720", 3000000, "http://example.com/stream2.m3u8", "audio_group1")
        );

        return ProbeDTO.builder()
                .traceId(traceId)
                .probeAt(Instant.now())
                .masterManifestUrl("http://example.com/master.m3u8")
                .userAgent("TestAgent/1.0")
                .format("hls")
                .durationSec(120.5)
                .overallBitrate(7000000)
                .saveM3u8State(SaveM3u8State.WITHOUT_ADSLATE)
                .streams(streams)
                .variants(variants)
                .build();
    }

    private SaveFailureDTO createTestSaveFailureDTO(String traceId) {
        return new SaveFailureDTO(
                traceId,
                ValidationResult.ERROR_STALL_NO_PROGRESS.name(),
                "Detailed human-readable reason for failure.",
                "1080p",
                Instant.now(),
                150L,
                125L,
                1L,
                Collections.emptyList(),
                6,
                "some-hash-value",
                "first_uri.ts",
                "last_uri.ts",
                List.of("uri1.ts", "uri2.ts"),
                false,
                124L,
                12345L,
                12346L,
                12346L
        );
    }
}
