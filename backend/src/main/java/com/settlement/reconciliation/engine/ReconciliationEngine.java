package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.domain.TxnType;
import com.settlement.reconciliation.ingest.IngestResult;
import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.SettlementRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReconciliationEngine {

    private final ReconConfig config;

    public ReconciliationEngine() {
        this(ReconConfig.defaults());
    }

    public ReconciliationEngine(ReconConfig config) {
        this.config = config;
    }

    public ReconciliationResult reconcile(IngestResult ingest) {
        return reconcile(ingest.internalTransactions(), ingest.settlementRecords());
    }

    public ReconciliationResult reconcile(
            List<InternalTransaction> internals,
            List<SettlementRecord> settlements
    ) {
        List<InternalTxnMatch> internalMatches = internals.stream()
                .map(InternalTxnMatch::new)
                .toList();
        List<SettlementMatch> settlementMatches = settlements.stream()
                .map(SettlementMatch::new)
                .toList();

        SettlementMatcher matcher = new SettlementMatcher(config);
        matcher.matchByReference(internalMatches, settlementMatches);
        matcher.matchBlankReferences(internalMatches, settlementMatches);

        Set<String> saleRefs = new HashSet<>();
        for (InternalTransaction txn : internals) {
            if (txn.type() == TxnType.SALE) {
                saleRefs.add(txn.merchantRef());
            }
        }

        OutcomeClassifier classifier = new OutcomeClassifier(config);
        List<InternalOutcome> outcomes = new ArrayList<>();
        for (InternalTxnMatch match : internalMatches) {
            outcomes.add(classifier.classify(match, saleRefs));
        }

        List<SettlementRecord> unmatched = settlementMatches.stream()
                .filter(row -> !row.taken())
                .map(SettlementMatch::record)
                .toList();

        return new ReconciliationResult(outcomes, unmatched);
    }
}
