package AutoMonitoring.AutoMonitoring;

import AutoMonitoring.AutoMonitoring.util.path.StorageProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProps.class)
public class AutoMonitoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoMonitoringApplication.class, args);
	}

}
