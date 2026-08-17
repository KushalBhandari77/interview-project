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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recon_run")
public class ReconRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "run_at", nullable = false)
    private Instant runAt;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal tolerance;

    @Column(name = "window_min", nullable = false)
    private int windowMin;

    @Column(name = "window_max", nullable = false)
    private int windowMax;

    protected ReconRunEntity() {
    }

    public ReconRunEntity(ImportBatch batch, Instant runAt, BigDecimal tolerance, int windowMin, int windowMax) {
        this.batch = batch;
        this.runAt = runAt;
        this.tolerance = tolerance;
        this.windowMin = windowMin;
        this.windowMax = windowMax;
    }

    public Long getId() {
        return id;
    }

    public ImportBatch getBatch() {
        return batch;
    }

    public Instant getRunAt() {
        return runAt;
    }

    public BigDecimal getTolerance() {
        return tolerance;
    }

    public int getWindowMin() {
        return windowMin;
    }

    public int getWindowMax() {
        return windowMax;
    }
}
