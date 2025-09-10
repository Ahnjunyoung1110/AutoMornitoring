package AutoMonitoring.AutoMonitoring.domain.db.repository;

import AutoMonitoring.AutoMonitoring.domain.db.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepo extends JpaRepository<Program, String> {

    public Program findByMasterManifestUrl(String master_manifest_url);
}
