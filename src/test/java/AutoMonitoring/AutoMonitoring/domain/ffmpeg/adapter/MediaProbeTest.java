package AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.domain.program.dto.ProbeDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")

class MediaProbeTest extends BaseTest {

    @Autowired
    MediaProbe mediaProbe;

    @Test
    void probe() {
        String url = "https://ads.its-newid.net/api/manifest.m3u8.php?tp=samsung_tvplus&channel_name=%EC%9A%B0%EB%A6%AC%EC%9D%98%EC%8B%9D%ED%83%81&channel_id=newid_016&apikey=b97c7a7d0c82424bb607e141ef215779&auth=4267aefa-e23c8733-cc1d493e-388f9d05&ads.live=[CONTENT_LIVE]&ads.device_did=97531fe2-5e9b-8ecf-48f4-8cdbeb3b982d&ads.ifa=[IFA]&ads.ifatype=[IFA_TYPE]&ads.lat=[LMT]&ads.donotsell=[DNS]&ads.ip=[IP]&ads.gdpr=[GDPR]&ads.gdprconsent=[GDPR_CONSENT]&ads.country=[COUNTRY]&ads.us_privacy=[US_PRIVACY]&ads.appstoreurl=[APP_STOREURL]&ads.bundleid=[APP_BUNDLE]&ads.app.name=%7BAPP_NAME%7D&ads.appversion=[APP_VERSION]&ads.devicetype=[DEVICE_TYPE]&ads.devicemake=[DEVICE_MAKE]&ads.devicemodel=[DEVICE_MODEL]&ads.targetad=[TARGETAD_ALLOWED]&ads.content_title=[PROGRAM_TITLE]&ads.content_series=[SERIES_TITLE]&ads.content_season=[SEASON_TITLE]&ads.content_episode=[EPISODE_NO]&ads.content_length=[CONTENT_LENGTH]&ads.device.dnt=0&ads.app_domain=%7BAPP_DOMAIN%7D&nh=true&ads.device_did=97531fe2-5e9b-8ecf-48f4-8cdbeb3b982d&ads.device_dnt=0&ads.app_domain=%7BAPP_DOMAIN%7D&ads.app_name=%7BAPP_NAME%7D&ads.ssai_vendor=SSSLIVE&ads.afsdk_params=%7BAFSDK_VALUE%7D&ads.service_id=KRBD2200001QD";
        ProbeDTO probeDTO = mediaProbe.probe(url, "");
                System.out.println(probeDTO);
        Assertions.assertThat(probeDTO.traceId()).isNotNull();
        System.out.println(probeDTO.traceId());
    }

    @Test
    void probe2() {
        String url = "https://ads.its-newid.net/api/manifest.m3u8?tp=lg_channels&channel_name=newkmovies&channel_id=newid_009&apikey=48230e6b-1cea0097-15975f93-39af1962&auth=00594230-f203384d-ebcd3217-8573a2e1&ads.live=[CONTENT_LIVE]&ads.deviceid=[DEVICE_ID]&ads.ifa=[IFA]&ads.ifatype=[IFA_TYPE]&ads.lat=[LMT]&ads.donotsell=[DNS]&ads.ip=[IP]&ads.gdpr=[GDPR]&ads.gdprconsent=[GDPR_CONSENT]&ads.country=[COUNTRY]&ads.us_privacy=[US_PRIVACY]&ads.appstoreurl=[APP_STOREURL]&ads.bundleid=[APP_BUNDLE]&ads.appname=[APP_NAME]&ads.appversion=[APP_VERSION]&ads.devicetype=[DEVICE_TYPE]&ads.devicemake=[DEVICE_MAKE]&ads.devicemodel=[DEVICE_MODEL]&ads.targetad=[TARGETAD_ALLOWED]&ads.content_title=[PROGRAM_TITLE]&ads.content_series=[SERIES_TITLE]&ads.content_season=[SEASON_TITLE]&ads.content_episode=[EPISODE_NO]&ads.content_length=[CONTENT_LENGTH]&ads.ua=[UA]";
        ProbeDTO probeDTO = mediaProbe.probe(url, "");
        System.out.println(probeDTO);
        Assertions.assertThat(probeDTO.traceId()).isNotNull();
        System.out.println(probeDTO.traceId());

    }
}