package AutoMonitoring.AutoMonitoring.domain.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class UrlValidateCheckTest {

    private final UrlValidateCheck urlValidateCheck = new UrlValidateCheck(HttpClient.newBuilder().build());

    @Test
    @DisplayName("실제 서비스에서 사용하는 긴 쿼리 파라미터를 가진 URL이 유효한지 검증한다.")
    void check_WithRealServiceUrl_ShouldReturnTrue() {
        // given
        String realServiceUrl = "https://ads.its-newid.net/api/manifest.m3u8.php?tp=lg_channels&channel_name=newkpop&channel_id=newid_001&mpf=b9b84d50-39af1962-bbea1f68&apikey=48230e6b-1cea0097-15975f93-39af1962&auth=5555ff2b-8870b9b2-25c0f07d-ff00231e&ads.live=0&ads.deviceid=caf2f188-6648-7855-5f94-63b4205cb2a9&ads.ifa=b9aa582b-1979-0056-65fd-d5fdd51759c2&ads.ifatype=lgwebostvadid&ads.lat=0&ads.donotsell=0&ads.ua=Mozilla/5.0%20(Web0S%3B%20Linux/SmartTV)%20AppleWebKit/537.36%20(KHTML%2C%20like%20Gecko)%20Chrome/68.0.3440.106%20Safari/537.36%20DMOST/2.0.0%20(%3B%20LGE%3B%20webOSTV%3B%20WEBOS5.6.0%2002.03.35%3B%20W5_o20%3B)&ads.ip=172.58.121.23&ads.gdpr=0&ads.gdpr_consent=[GDPR_CONSENT]&ads.country=US&ads.us_privacy=1YNN&ads.appstoreurl=https%3A//us.lgappstv.com/main/tvapp/detail%3FappId%3D474301&ads.bundleid=474301&ads.appname=lgchannels&ads.appversion=1.2.83&ads.devicetype=3-connected_tv&ads.devicemake=LG_LGELECTRONICS_LG&ads.devicemodel=55UQ7050ZUD&ads.targetad=1&ads.fck=511&ads.viewsize=1&ads.givn=0";

        // when
        boolean isValid = urlValidateCheck.check(realServiceUrl);

        // then
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "just-a-string", "ftp://invalid.com", "www.google.com"})
    @DisplayName("유효하지 않은 형식의 URL을 전달하면 false를 반환한다.")
    void check_WithInvalidUrls_ShouldReturnFalse(String invalidUrl) {
        // when
        boolean isValid = urlValidateCheck.check(invalidUrl);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("null을 전달하면 false를 반환한다.")
    void check_WithNullUrl_ShouldReturnFalse() {
        // when
        boolean isValid = urlValidateCheck.check(null);

        // then
        assertThat(isValid).isFalse();
    }
}