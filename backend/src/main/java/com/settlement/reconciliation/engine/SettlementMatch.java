package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.ingest.SettlementRecord;

final class SettlementMatch {

    private final SettlementRecord record;
    private boolean taken;

    SettlementMatch(SettlementRecord record) {
        this.record = record;
    }

    SettlementRecord record() {
        return record;
    }

    boolean taken() {
        return taken;
    }

    void markTaken() {
        taken = true;
    }
}
