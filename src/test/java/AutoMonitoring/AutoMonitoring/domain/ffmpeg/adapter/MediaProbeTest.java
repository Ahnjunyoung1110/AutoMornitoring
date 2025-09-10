package AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter;

import AutoMonitoring.AutoMonitoring.domain.dto.ProbeDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class MediaProbeTest {

    @Autowired
    MediaProbe mediaProbe;

    @Test
    void probe() {
        ProbeDTO probeDTO = mediaProbe.probe("https://live.its-newid.net/live/newid_184/lg_channels/index.m3u8?apikey=48230e6b-1cea0097-15975f93-39af1962&auth=f0134da5-c23d180a-6860fff6-617fe589");
        System.out.println(probeDTO);
        Assertions.assertThat(probeDTO.traceId()).isNotNull();
        System.out.println(probeDTO.traceId());
    }
}