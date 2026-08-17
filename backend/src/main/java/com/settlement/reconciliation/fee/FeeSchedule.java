package com.settlement.reconciliation.fee;

import com.settlement.reconciliation.domain.CardType;

import java.util.Map;

public record FeeSchedule(
        Map<CardType, FeeRate> interchange,
        FeeRate processorMarkup
) {
}
