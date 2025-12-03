package AutoMonitoring.AutoMonitoring.domain.program.repository;

import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.VariantInfoEmb;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepo extends JpaRepository<Program, String> {

    public Program findByMasterManifestUrl(String master_manifest_url);
    Optional<Program> findByTraceId(String traceId);


    @Query("""
            select v
            from Program p
            join p.variants v
            where p.traceId = :traceId
            
            
            
            """)
    Optional<List<VariantInfoEmb>> findVarient(@Param("traceId") String traceId);
}
