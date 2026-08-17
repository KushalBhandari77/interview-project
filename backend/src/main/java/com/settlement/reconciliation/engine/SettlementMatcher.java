package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.domain.TxnType;
import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.SettlementRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SettlementMatcher {

    private final ReconConfig config;

    SettlementMatcher(ReconConfig config) {
        this.config = config;
    }

    void matchByReference(List<InternalTxnMatch> internals, List<SettlementMatch> settlements) {
        Map<String, List<InternalTxnMatch>> byRef = new HashMap<>();
        for (InternalTxnMatch internal : internals) {
            byRef.computeIfAbsent(internal.txn().merchantRef(), ref -> new ArrayList<>()).add(internal);
        }

        for (SettlementMatch settlement : settlements) {
            SettlementRecord row = settlement.record();
            if (isBlank(row.merchantRef())) {
                continue;
            }
            List<InternalTxnMatch> candidates = byRef.get(row.merchantRef());
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }

            boolean refund = isRefund(row);
            List<InternalTxnMatch> typed = candidates.stream()
                    .filter(internal -> isRefund(internal.txn()) == refund)
                    .toList();
            if (typed.isEmpty()) {
                continue;
            }

            InternalTxnMatch best = typed.stream()
                    .min(Comparator.comparing(internal -> netDistance(internal, row)))
                    .orElseThrow();
            best.addMatch(row);
            settlement.markTaken();
        }
    }

    void matchBlankReferences(List<InternalTxnMatch> internals, List<SettlementMatch> settlements) {
        for (SettlementMatch settlement : settlements) {
            if (settlement.taken()) {
                continue;
            }
            SettlementRecord row = settlement.record();
            if (!isBlank(row.merchantRef())) {
                continue;
            }

            boolean refund = isRefund(row);
            List<InternalTxnMatch> candidates = internals.stream()
                    .filter(internal -> !internal.hasMatches())
                    .filter(internal -> internal.txn().merchantId().equals(row.merchantId()))
                    .filter(internal -> internal.txn().cardType() == row.cardType())
                    .filter(internal -> internal.txn().cardLast4().equals(row.cardLast4()))
                    .filter(internal -> isRefund(internal.txn()) == refund)
                    .filter(internal -> withinTolerance(internal.txn().expectedNet().amount(), row.settledAmount().amount()))
                    .filter(internal -> !row.settlementDate().isBefore(captureDate(internal.txn())))
                    .toList();

            if (candidates.isEmpty()) {
                continue;
            }

            InternalTxnMatch best = candidates.stream()
                    .min(Comparator.comparing(internal -> dayOffset(internal, row)))
                    .orElseThrow();
            best.addMatch(row);
            settlement.markTaken();
        }
    }

    private BigDecimal netDistance(InternalTxnMatch internal, SettlementRecord row) {
        return internal.txn().expectedNet().amount().subtract(row.settledAmount().amount()).abs();
    }

    private int dayOffset(InternalTxnMatch internal, SettlementRecord row) {
        return (int) (row.settlementDate().toEpochDay() - captureDate(internal.txn()).toEpochDay());
    }

    private LocalDate captureDate(InternalTransaction txn) {
        return txn.capturedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private boolean isRefund(SettlementRecord row) {
        return row.settledAmount().amount().signum() < 0;
    }

    private boolean isRefund(InternalTransaction txn) {
        return txn.type() == TxnType.REFUND;
    }

    private boolean withinTolerance(BigDecimal left, BigDecimal right) {
        return left.subtract(right).abs().compareTo(config.tolerance()) <= 0;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
