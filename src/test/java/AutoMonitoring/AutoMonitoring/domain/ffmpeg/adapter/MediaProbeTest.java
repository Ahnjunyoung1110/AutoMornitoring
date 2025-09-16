package AutoMonitoring.AutoMonitoring.domain.ffmpeg.adapter;

import AutoMonitoring.AutoMonitoring.domain.program.dto.ProbeDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MediaProbeTest {

    @Autowired
    MediaProbe mediaProbe;

    @Test
    void probe() {
        ProbeDTO probeDTO = mediaProbe.probe("https://ads.its-newid.net/api/manifest.m3u8?tp=lg_channels&channel_name=고독한미식가&channel_id=newid_091&mpf=687af619-39af1962-d77ce6a6&apikey=48230e6b-1cea0097-15975f93-39af1962&auth=f637d864-f0e39a59-0745dbf5-53a167da&ads.live=1&ads.deviceid=e924943d-4c9a-8553-6455-ee6aab7e155a&ads.ifa=2443a3c5-5e4e-565a-4a63-022848e9a4c0&ads.ifatype=lgwebostvadid&ads.lat=0&ads.donotsell=&ads.ua=Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.34 Safari/537.36 DMOST/2.0.1 (; LGE; webOSTV; WEBOS4.10.1 05.40.90; W4_m16p3;) &ads.ip=125.130.79.4&ads.gdpr=0&ads.gdpr_consent=&ads.country=KR&ads.us_privacy=&ads.appstoreurl=https://kr.lgappstv.com/main/tvapp/detail?appId=948993&ads.bundleid=948993&ads.appname=lgchannels&ads.appversion=3.3.36-2&ads.devicetype=Connected TV&ads.devicemake=LG_ELECTRONICS_LG&ads.devicemodel=OLED55B9FNA&ads.targetad=1&ads.fck=171&ads.viewsize=0\n", "");
                System.out.println(probeDTO);
        Assertions.assertThat(probeDTO.traceId()).isNotNull();
        System.out.println(probeDTO.traceId());
    }
}