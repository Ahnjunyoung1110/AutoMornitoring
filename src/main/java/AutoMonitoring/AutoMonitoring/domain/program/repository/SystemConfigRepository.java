package AutoMonitoring.AutoMonitoring.domain.program.repository;

import AutoMonitoring.AutoMonitoring.domain.program.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
}
