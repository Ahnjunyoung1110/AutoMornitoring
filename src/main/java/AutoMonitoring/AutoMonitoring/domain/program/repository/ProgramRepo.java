package AutoMonitoring.AutoMonitoring.domain.program.repository;

import AutoMonitoring.AutoMonitoring.contract.program.ResolutionStatus;
import AutoMonitoring.AutoMonitoring.domain.program.entity.Program;
import AutoMonitoring.AutoMonitoring.domain.program.entity.VariantInfoEmb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT DISTINCT p FROM Program p JOIN p.variants v WHERE v.status = 'FAILED'
    """)
    List<Program> findAllByVariantStatusFailed();

    @Query(value = "SELECT DISTINCT p FROM Program p JOIN p.variants v " +
                   "WHERE (:resolutionStatus IS NULL OR v.status = :resolutionStatus) " + // Filter by selected status
                   "AND (COALESCE(:traceId, '') = '' OR p.traceId LIKE %:traceId%) " +
                   "AND (COALESCE(:channelId, '') = '' OR p.channelId LIKE %:channelId%) " +
                   "AND (COALESCE(:tp, '') = '' OR p.tp LIKE %:tp%)",
           countQuery = "SELECT COUNT(DISTINCT p) FROM Program p JOIN p.variants v " +
                        "WHERE (:resolutionStatus IS NULL OR v.status = :resolutionStatus) " +
                        "AND (COALESCE(:traceId, '') = '' OR p.traceId LIKE %:traceId%) " +
                        "AND (COALESCE(:channelId, '') = '' OR p.channelId LIKE %:channelId%) " +
                        "AND (COALESCE(:tp, '') = '' OR p.tp LIKE %:tp%)")
    Page<Program> findMonitoring(
        @Param("resolutionStatus") ResolutionStatus resolutionStatus,
        @Param("traceId") String traceId,
        @Param("channelId") String channelId,
        @Param("tp") String tp,
        Pageable pageable
    );
}
