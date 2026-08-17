package com.settlement.reconciliation.api.dto;

import java.math.BigDecimal;

public record MerchantRollup(
        String merchantId,
        long matchedCount,
        long breakCount,
        BigDecimal breakAmount
) {
}
