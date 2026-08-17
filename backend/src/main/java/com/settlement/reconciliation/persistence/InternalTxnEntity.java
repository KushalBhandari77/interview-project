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
@Table(name = "internal_transaction")
public class InternalTxnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "internal_txn_id", nullable = false, length = 64)
    private String internalTxnId;

    @Column(name = "merchant_id", nullable = false, length = 32)
    private String merchantId;

    @Column(name = "merchant_ref", nullable = false, length = 64)
    private String merchantRef;

    @Column(name = "card_type", nullable = false, length = 16)
    private String cardType;

    @Column(name = "card_last4", nullable = false, length = 8)
    private String cardLast4;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "txn_type", nullable = false, length = 8)
    private String txnType;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "expected_interchange", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedInterchange;

    @Column(name = "expected_processor", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedProcessor;

    @Column(name = "expected_net", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedNet;

    protected InternalTxnEntity() {
    }

    public InternalTxnEntity(
            ImportBatch batch,
            String internalTxnId,
            String merchantId,
            String merchantRef,
            String cardType,
            String cardLast4,
            BigDecimal grossAmount,
            String txnType,
            Instant capturedAt,
            BigDecimal expectedInterchange,
            BigDecimal expectedProcessor,
            BigDecimal expectedNet
    ) {
        this.batch = batch;
        this.internalTxnId = internalTxnId;
        this.merchantId = merchantId;
        this.merchantRef = merchantRef;
        this.cardType = cardType;
        this.cardLast4 = cardLast4;
        this.grossAmount = grossAmount;
        this.txnType = txnType;
        this.capturedAt = capturedAt;
        this.expectedInterchange = expectedInterchange;
        this.expectedProcessor = expectedProcessor;
        this.expectedNet = expectedNet;
    }

    public Long getId() {
        return id;
    }

    public ImportBatch getBatch() {
        return batch;
    }

    public String getInternalTxnId() {
        return internalTxnId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantRef() {
        return merchantRef;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public String getTxnType() {
        return txnType;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public BigDecimal getExpectedInterchange() {
        return expectedInterchange;
    }

    public BigDecimal getExpectedProcessor() {
        return expectedProcessor;
    }

    public BigDecimal getExpectedNet() {
        return expectedNet;
    }
}
