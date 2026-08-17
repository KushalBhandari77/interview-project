package com.settlement.reconciliation.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReconOutcomeRepository extends JpaRepository<ReconOutcomeEntity, Long> {

    List<ReconOutcomeEntity> findByRun_Id(Long runId);

    @Query("SELECT o.outcome, COUNT(o) FROM ReconOutcomeEntity o WHERE o.run.id = :runId GROUP BY o.outcome")
    List<Object[]> countByOutcome(@Param("runId") Long runId);

    @Query("""
            SELECT o FROM ReconOutcomeEntity o
            LEFT JOIN FETCH o.internalTransaction
            WHERE o.run.id = :runId
            AND o.outcome <> 'MATCHED'
            AND (:outcome IS NULL OR o.outcome = :outcome)
            AND (
                :merchantId IS NULL
                OR o.internalTransaction.merchantId = :merchantId
                OR EXISTS (
                    SELECT 1 FROM ReconOutcomeSettlementEntity os
                    JOIN os.settlement s
                    WHERE os.outcome = o AND s.merchantId = :merchantId
                )
            )
            """)
    Page<ReconOutcomeEntity> findBreaks(
            @Param("runId") Long runId,
            @Param("outcome") String outcome,
            @Param("merchantId") String merchantId,
            Pageable pageable
    );

    @Query("""
            SELECT o FROM ReconOutcomeEntity o
            LEFT JOIN FETCH o.internalTransaction
            WHERE o.id = :id
            """)
    Optional<ReconOutcomeEntity> findByIdWithInternal(@Param("id") Long id);
}
