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
@Table(name = "recon_outcome")
public class ReconOutcomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconRunEntity run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_transaction_id")
    private InternalTxnEntity internalTransaction;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(length = 500)
    private String detail;

    @Column(name = "settlement_day_offset")
    private Integer settlementDayOffset;

    protected ReconOutcomeEntity() {
    }

    public ReconOutcomeEntity(
            ReconRunEntity run,
            InternalTxnEntity internalTransaction,
            String outcome,
            String detail,
            Integer settlementDayOffset
    ) {
        this.run = run;
        this.internalTransaction = internalTransaction;
        this.outcome = outcome;
        this.detail = detail;
        this.settlementDayOffset = settlementDayOffset;
    }

    public Long getId() {
        return id;
    }

    public ReconRunEntity getRun() {
        return run;
    }

    public InternalTxnEntity getInternalTransaction() {
        return internalTransaction;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getDetail() {
        return detail;
    }

    public Integer getSettlementDayOffset() {
        return settlementDayOffset;
    }
}
