package com.settlement.reconciliation.api.dto;

import java.math.BigDecimal;

public record CategorySummary(
        String outcome,
        long count,
        BigDecimal totalAmount
) {
}
