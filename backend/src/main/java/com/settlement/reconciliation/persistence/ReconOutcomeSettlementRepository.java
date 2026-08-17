package com.settlement.reconciliation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReconOutcomeSettlementRepository extends JpaRepository<ReconOutcomeSettlementEntity, Long> {

    List<ReconOutcomeSettlementEntity> findByOutcome_Id(Long outcomeId);

    @Query("""
            SELECT os FROM ReconOutcomeSettlementEntity os
            JOIN FETCH os.settlement
            WHERE os.outcome.id IN :outcomeIds
            """)
    List<ReconOutcomeSettlementEntity> findByOutcomeIdInWithSettlement(
            @Param("outcomeIds") Collection<Long> outcomeIds
    );
}
