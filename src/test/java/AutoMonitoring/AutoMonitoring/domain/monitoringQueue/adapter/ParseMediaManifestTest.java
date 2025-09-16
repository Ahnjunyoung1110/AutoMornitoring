package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.adapter;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.ParseMediaManifestImpl;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.application.SnapshotStore;
import AutoMonitoring.AutoMonitoring.util.redis.dto.RecordMediaToRedisDTO;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;


@SpringBootTest
@RequiredArgsConstructor
class ParseMediaManifestTest {

    private final ParseMediaManifest parseMediaManifest;



    @Test
    void parse() {
        String testManifest = """
                #EXTM3U
                #EXT-X-VERSION:6
                #EXT-X-MEDIA-SEQUENCE:10462006
                #EXT-X-TARGETDURATION:5
                #EXT-X-DISCONTINUITY-SEQUENCE:207886
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0158.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0159.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0160.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0161.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0162.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0163.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0164.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0165.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0166.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0167.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                """;

        RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(testManifest, Duration.ZERO,"123", "1080");


        Assertions.assertThat(recordMediaToRedisDTO.seq()).isEqualTo(10462006);
        Assertions.assertThat(recordMediaToRedisDTO.dseq()).isEqualTo(207886);
        Assertions.assertThat(recordMediaToRedisDTO.segFirstUri()).isEqualTo("https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0158.ts");
        Assertions.assertThat(recordMediaToRedisDTO.segLastUri()).isEqualTo("https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0167.ts");
        Assertions.assertThat(recordMediaToRedisDTO.segmentCount()).isEqualTo(10);
        Assertions.assertThat(recordMediaToRedisDTO.wrongExtinf()).isEqualTo(false);
    }


    // discontinuity 가 존재하는 경우 이를 인식하는지 확인하는 테스트
    @Test
    void parseWithDiscon(){
        String testManifest = """
                #EXTM3U
                #EXT-X-VERSION:6
                #EXT-X-MEDIA-SEQUENCE:10462006
                #EXT-X-TARGETDURATION:5
                #EXT-X-DISCONTINUITY-SEQUENCE:207886
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0158.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0159.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0160.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0161.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0161.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXT-X-DISCONTINUITY
                #EXTINF:5.0,
                https://qwer
                """;

        RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(testManifest, Duration.ZERO, "123", "1080");

        Assertions.assertThat(recordMediaToRedisDTO.disCount()).isEqualTo(1);
        Assertions.assertThat(recordMediaToRedisDTO.wrongExtinf()).isEqualTo(false);
        Assertions.assertThat(recordMediaToRedisDTO.segmentCount()).isEqualTo(6);
    }

    //  EXFINF 가 5가 아니고 EXT-X-DISCONTINUITY가 없는경우 woringExtinf가 true 이어야함
    @Test
    void parseWithWrong(){
        String testManifest = """
                #EXTM3U
                #EXT-X-VERSION:6
                #EXT-X-MEDIA-SEQUENCE:10462006
                #EXT-X-TARGETDURATION:5
                #EXT-X-DISCONTINUITY-SEQUENCE:207886
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0158.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0159.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0160.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0161.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:3.1,
                https://cdn88.its-newid.net/asset/009f347c012baf3bd8696a027b05017c333e0716/hevc-448bc513-f37f735b-ad4302bb-bf13f04c0-ts0161.ts?channel_id=newid_091&target_platform=lg_channels&resolution=1920x1080&program_id=009f347c012baf3bd8696a027b05017c333e0716
                #EXTINF:5.0,
                https://qwer
                """;

        RecordMediaToRedisDTO recordMediaToRedisDTO = parseMediaManifest.parse(testManifest, Duration.ZERO, "123", "1080");

        Assertions.assertThat(recordMediaToRedisDTO.disCount()).isEqualTo(0);
        Assertions.assertThat(recordMediaToRedisDTO.wrongExtinf()).isEqualTo(true);
        Assertions.assertThat(recordMediaToRedisDTO.segmentCount()).isEqualTo(6);

    }

}