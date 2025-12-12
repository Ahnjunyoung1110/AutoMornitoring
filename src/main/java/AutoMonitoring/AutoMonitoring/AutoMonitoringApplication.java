package AutoMonitoring.AutoMonitoring;

import AutoMonitoring.AutoMonitoring.util.path.StorageProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableConfigurationProperties(StorageProps.class)
@EnableFeignClients
public class AutoMonitoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoMonitoringApplication.class, args);
	}

}
