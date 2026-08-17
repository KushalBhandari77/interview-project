package com.settlement.reconciliation.ingest;

public record SettlementRow(
        String networkRef,
        String merchantRef,
        String merchantId,
        String cardType,
        String cardLast4,
        String settledAmount,
        String interchangeFee,
        String processorFee,
        String currency,
        String settlementDate
) {
}
