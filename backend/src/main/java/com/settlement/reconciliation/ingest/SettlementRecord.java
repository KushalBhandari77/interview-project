package com.settlement.reconciliation.ingest;

import com.settlement.reconciliation.domain.CardType;
import com.settlement.reconciliation.domain.Money;

import java.time.LocalDate;

public record SettlementRecord(
        String networkRef,
        String merchantRef,
        String merchantId,
        CardType cardType,
        String cardLast4,
        Money settledAmount,
        Money interchangeFee,
        Money processorFee,
        LocalDate settlementDate
) {
}
