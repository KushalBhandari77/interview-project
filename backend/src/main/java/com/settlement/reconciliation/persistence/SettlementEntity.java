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
import java.time.LocalDate;

@Entity
@Table(name = "settlement_record")
public class SettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "network_ref", nullable = false, length = 64)
    private String networkRef;

    @Column(name = "merchant_ref", nullable = false, length = 64)
    private String merchantRef;

    @Column(name = "merchant_id", nullable = false, length = 32)
    private String merchantId;

    @Column(name = "card_type", nullable = false, length = 16)
    private String cardType;

    @Column(name = "card_last4", nullable = false, length = 8)
    private String cardLast4;

    @Column(name = "settled_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal settledAmount;

    @Column(name = "interchange_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal interchangeFee;

    @Column(name = "processor_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal processorFee;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    protected SettlementEntity() {
    }

    public SettlementEntity(
            ImportBatch batch,
            String networkRef,
            String merchantRef,
            String merchantId,
            String cardType,
            String cardLast4,
            BigDecimal settledAmount,
            BigDecimal interchangeFee,
            BigDecimal processorFee,
            LocalDate settlementDate
    ) {
        this.batch = batch;
        this.networkRef = networkRef;
        this.merchantRef = merchantRef;
        this.merchantId = merchantId;
        this.cardType = cardType;
        this.cardLast4 = cardLast4;
        this.settledAmount = settledAmount;
        this.interchangeFee = interchangeFee;
        this.processorFee = processorFee;
        this.settlementDate = settlementDate;
    }

    public Long getId() {
        return id;
    }

    public ImportBatch getBatch() {
        return batch;
    }

    public String getNetworkRef() {
        return networkRef;
    }

    public String getMerchantRef() {
        return merchantRef;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    public BigDecimal getInterchangeFee() {
        return interchangeFee;
    }

    public BigDecimal getProcessorFee() {
        return processorFee;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }
}
