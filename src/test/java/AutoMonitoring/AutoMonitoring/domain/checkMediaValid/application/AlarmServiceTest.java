package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;

import AutoMonitoring.AutoMonitoring.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AlarmServiceTest extends BaseTest {
    @Autowired
    private AlarmService alarmService;    @Test
    void publishAlarm() {
        alarmService.publishAlarm("ㄹㅇ이다;", "1080", "qwer");

    }
}