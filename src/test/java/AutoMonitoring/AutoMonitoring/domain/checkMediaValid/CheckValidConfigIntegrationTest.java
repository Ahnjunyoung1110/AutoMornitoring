package AutoMonitoring.AutoMonitoring.domain.checkMediaValid;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.UpdateAlarmConfigCommand;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.util.CheckValidConfigHolder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckValidConfigIntegrationTest extends BaseTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CheckValidConfigHolder checkValidConfigHolder;

    @Test
    void updateAlarmConfigCommand_updatesHolder() throws InterruptedException {
        // Arrange
        UpdateAlarmConfigCommand command = UpdateAlarmConfigCommand.builder()
                .alarmEnabled(false)
                .threshold(20)
                .alarmCooldownSeconds(300)
                .build();

        // Act
        rabbitTemplate.convertAndSend(RabbitNames.EX_CHECKVALID_COMMAND, RabbitNames.RK_CHECKVALID_COMMAND, command);

        // Assert - Wait for the message to be consumed and holder updated
        TimeUnit.SECONDS.sleep(2); // Give some time for RabbitMQ to process

        assertThat(checkValidConfigHolder.getAlarmEnabled().get()).isEqualTo(command.alarmEnabled());
        assertThat(checkValidConfigHolder.getThreshold().get()).isEqualTo(command.threshold());
        assertThat(checkValidConfigHolder.getAlarmCooldownSeconds().get()).isEqualTo(command.alarmCooldownSeconds());
    }
}
