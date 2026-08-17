package com.settlement.reconciliation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internal_hash", nullable = false, length = 64)
    private String internalHash;

    @Column(name = "settlement_hash", nullable = false, length = 64)
    private String settlementHash;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "internal_count", nullable = false)
    private int internalCount;

    @Column(name = "settlement_count", nullable = false)
    private int settlementCount;

    @Column(name = "quarantined_count", nullable = false)
    private int quarantinedCount;

    protected ImportBatch() {
    }

    public ImportBatch(
            String internalHash,
            String settlementHash,
            Instant importedAt,
            int internalCount,
            int settlementCount,
            int quarantinedCount
    ) {
        this.internalHash = internalHash;
        this.settlementHash = settlementHash;
        this.importedAt = importedAt;
        this.internalCount = internalCount;
        this.settlementCount = settlementCount;
        this.quarantinedCount = quarantinedCount;
    }

    public Long getId() {
        return id;
    }

    public String getInternalHash() {
        return internalHash;
    }

    public String getSettlementHash() {
        return settlementHash;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public int getInternalCount() {
        return internalCount;
    }

    public int getSettlementCount() {
        return settlementCount;
    }

    public int getQuarantinedCount() {
        return quarantinedCount;
    }
}
