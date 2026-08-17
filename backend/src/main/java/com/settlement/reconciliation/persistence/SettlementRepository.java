package com.settlement.reconciliation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SettlementRepository extends JpaRepository<SettlementEntity, Long> {

    List<SettlementEntity> findByBatch_Id(Long batchId);

    @Query("SELECT COALESCE(SUM(s.settledAmount), 0) FROM SettlementEntity s WHERE s.batch.id = :batchId")
    BigDecimal sumSettledAmount(@Param("batchId") Long batchId);

    @Query("""
            SELECT COALESCE(SUM(s.interchangeFee + s.processorFee), 0)
            FROM SettlementEntity s WHERE s.batch.id = :batchId
            """)
    BigDecimal sumTotalFees(@Param("batchId") Long batchId);
}
