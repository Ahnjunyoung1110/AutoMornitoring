package AutoMonitoring.AutoMonitoring.domain.program.repository;

import AutoMonitoring.AutoMonitoring.domain.program.entity.ValidationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * M3U8 유효성 검사 실패 이력 엔티티 {@link ValidationLog}의 리포지토리 인터페이스
 */
@Repository
public interface ValidationLogRepo extends JpaRepository<ValidationLog, String> {
}
