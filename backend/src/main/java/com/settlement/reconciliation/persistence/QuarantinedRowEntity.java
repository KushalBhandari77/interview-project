package com.settlement.reconciliation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quarantined_row")
public class QuarantinedRowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(nullable = false, length = 16)
    private String side;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "source_id", length = 64)
    private String sourceId;

    @Column(name = "raw_payload", nullable = false, length = 4000)
    private String rawPayload;

    @Column(nullable = false, length = 32)
    private String reason;

    protected QuarantinedRowEntity() {
    }

    public QuarantinedRowEntity(
            ImportBatch batch,
            String side,
            int lineNumber,
            String sourceId,
            String rawPayload,
            String reason
    ) {
        this.batch = batch;
        this.side = side;
        this.lineNumber = lineNumber;
        this.sourceId = sourceId;
        this.rawPayload = rawPayload;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public ImportBatch getBatch() {
        return batch;
    }

    public String getSide() {
        return side;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getReason() {
        return reason;
    }
}
