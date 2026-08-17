package com.settlement.reconciliation.api.dto;

import java.math.BigDecimal;

public record BreakSummary(
        Long outcomeId,
        String outcome,
        String merchantId,
        String internalTxnId,
        String merchantRef,
        BigDecimal amount,
        String detail,
        int settlementCount
) {
}
