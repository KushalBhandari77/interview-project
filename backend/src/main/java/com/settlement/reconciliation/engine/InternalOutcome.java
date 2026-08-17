package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.SettlementRecord;

import java.util.List;

public record InternalOutcome(
        InternalTransaction internal,
        OutcomeType outcome,
        List<SettlementRecord> settlements,
        String detail,
        Integer settlementDayOffset
) {
}
