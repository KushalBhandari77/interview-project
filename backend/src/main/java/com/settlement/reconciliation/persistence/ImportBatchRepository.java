package com.settlement.reconciliation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    Optional<ImportBatch> findByInternalHashAndSettlementHash(String internalHash, String settlementHash);

    long count();
}
