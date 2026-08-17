package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.SettlementRecord;

import java.util.ArrayList;
import java.util.List;

final class InternalTxnMatch {

    private final InternalTransaction txn;
    private final List<SettlementRecord> settlements = new ArrayList<>();

    InternalTxnMatch(InternalTransaction txn) {
        this.txn = txn;
    }

    InternalTransaction txn() {
        return txn;
    }

    List<SettlementRecord> settlements() {
        return List.copyOf(settlements);
    }

    boolean hasMatches() {
        return !settlements.isEmpty();
    }

    void addMatch(SettlementRecord settlement) {
        settlements.add(settlement);
    }
}
