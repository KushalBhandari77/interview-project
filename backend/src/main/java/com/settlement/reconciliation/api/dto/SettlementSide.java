package com.settlement.reconciliation.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementSide(
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
}
