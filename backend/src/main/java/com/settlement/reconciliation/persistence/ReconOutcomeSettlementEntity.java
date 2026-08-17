package com.settlement.reconciliation.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recon_outcome_settlement")
public class ReconOutcomeSettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outcome_id", nullable = false)
    private ReconOutcomeEntity outcome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private SettlementEntity settlement;

    protected ReconOutcomeSettlementEntity() {
    }

    public ReconOutcomeSettlementEntity(ReconOutcomeEntity outcome, SettlementEntity settlement) {
        this.outcome = outcome;
        this.settlement = settlement;
    }

    public ReconOutcomeEntity getOutcome() {
        return outcome;
    }

    public SettlementEntity getSettlement() {
        return settlement;
    }
}
