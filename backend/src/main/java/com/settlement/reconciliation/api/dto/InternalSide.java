package com.settlement.reconciliation.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InternalSide(
        String internalTxnId,
        String merchantId,
        String merchantRef,
        String cardType,
        String cardLast4,
        String txnType,
        BigDecimal grossAmount,
        BigDecimal expectedInterchange,
        BigDecimal expectedProcessor,
        BigDecimal expectedNet,
        Instant capturedAt
) {
}
