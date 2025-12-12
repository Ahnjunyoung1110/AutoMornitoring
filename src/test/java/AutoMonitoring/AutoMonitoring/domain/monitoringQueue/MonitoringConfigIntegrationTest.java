package AutoMonitoring.AutoMonitoring.domain.monitoringQueue;

import AutoMonitoring.AutoMonitoring.BaseTest;
import AutoMonitoring.AutoMonitoring.config.RabbitNames;
import AutoMonitoring.AutoMonitoring.contract.monitoringQueue.QueueSystemConfigCommand;
import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util.MonitoringConfigHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.fail;

@ActiveProfiles("test") // Use application-test.yml for testing
public class MonitoringConfigIntegrationTest  extends BaseTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MonitoringConfigHolder monitoringConfigHolder;

    @AfterEach
    public void afterTest(){
        // reset
        QueueSystemConfigCommand command = QueueSystemConfigCommand.builder()
                .reconnectThreshold(10)
                .reconnectTimeoutMillis(5000)
                .reconnectRetryDelayMillis(2000)
                .httpRequestTimeoutMillis(2000)
                .autoRefresh(false)
                .monitoringEnabled(true)
                .build();

        rabbitTemplate.convertAndSend(RabbitNames.EX_MONITORING_COMMAND, RabbitNames.RK_MONITORING_COMMAND, command);

    }
    @Test
    void queueSystemConfigCommand_updatesHolder() throws InterruptedException {
        // Arrange
        QueueSystemConfigCommand command = QueueSystemConfigCommand.builder()
                .reconnectThreshold(50)
                .reconnectTimeoutMillis(10000)
                .reconnectRetryDelayMillis(5000)
                .httpRequestTimeoutMillis(8000)
                .autoRefresh(false)
                .monitoringEnabled(false)
                .build();

        // Act
        rabbitTemplate.convertAndSend(RabbitNames.EX_MONITORING_COMMAND, RabbitNames.RK_MONITORING_COMMAND, command);

        // Assert - Wait for the message to be consumed and holder updated
        waitUntilUpdated(command, 6000L);



    }


    private void waitUntilUpdated(QueueSystemConfigCommand command, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            boolean done =
                    monitoringConfigHolder.getReconnectThreshold().get() == command.reconnectThreshold() &&
                            monitoringConfigHolder.getReconnectTimeoutMillis().get() == command.reconnectTimeoutMillis() &&
                            monitoringConfigHolder.getReconnectRetryDelayMillis().get() == command.reconnectRetryDelayMillis() &&
                            monitoringConfigHolder.getHttpRequestTimeoutMillis().get() == command.httpRequestTimeoutMillis() &&
                            monitoringConfigHolder.getAutoRefresh().get() == command.autoRefresh() &&
                            monitoringConfigHolder.getMonitoringEnabled().get() == command.monitoringEnabled();

            if (done) {
                return;
            }

            Thread.sleep(50); // 50ms 간격으로 다시 확인
        }

        fail("monitoringConfigHolder was not updated within " + timeoutMillis + " ms");
    }
}
