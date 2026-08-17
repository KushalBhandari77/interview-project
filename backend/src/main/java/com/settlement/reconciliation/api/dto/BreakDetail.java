package com.settlement.reconciliation.api.dto;

import java.util.List;

public record BreakDetail(
        Long outcomeId,
        String outcome,
        String detail,
        Integer settlementDayOffset,
        InternalSide internal,
        List<SettlementSide> settlements
) {
}
