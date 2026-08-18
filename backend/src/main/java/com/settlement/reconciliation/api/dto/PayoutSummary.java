package com.settlement.reconciliation.api.dto;

import java.math.BigDecimal;

public record PayoutSummary(
        BigDecimal expectedPayout,
        BigDecimal actualSettled,
        BigDecimal discrepancy,
        BigDecimal totalFees,
        BigDecimal saleGross,
        BigDecimal refundGross
) {
}
