package com.settlement.reconciliation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface InternalTxnRepository extends JpaRepository<InternalTxnEntity, Long> {

    List<InternalTxnEntity> findByBatch_Id(Long batchId);

    @Query("SELECT COALESCE(SUM(i.expectedNet), 0) FROM InternalTxnEntity i WHERE i.batch.id = :batchId")
    BigDecimal sumExpectedNet(@Param("batchId") Long batchId);
}
