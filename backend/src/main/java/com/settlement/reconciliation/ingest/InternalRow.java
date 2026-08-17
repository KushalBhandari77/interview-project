package com.settlement.reconciliation.ingest;

public record InternalRow(
        String internalTxnId,
        String merchantId,
        String merchantRef,
        String cardType,
        String cardLast4,
        String grossAmount,
        String currency,
        String type,
        String capturedAt
) {
}
