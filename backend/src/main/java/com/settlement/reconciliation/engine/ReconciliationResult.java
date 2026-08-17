package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.SettlementRecord;

import java.util.List;

public record ReconciliationResult(
        List<InternalOutcome> internalOutcomes,
        List<SettlementRecord> unmatchedSettlements
) {
}
