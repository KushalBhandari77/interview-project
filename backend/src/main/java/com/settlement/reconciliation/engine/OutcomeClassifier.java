package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.domain.TxnType;
import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.SettlementRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class OutcomeClassifier {

    private final ReconConfig config;

    OutcomeClassifier(ReconConfig config) {
        this.config = config;
    }

    InternalOutcome classify(InternalTxnMatch match, Set<String> saleRefs) {
        InternalTransaction txn = match.txn();
        List<SettlementRecord> settlements = match.settlements();

        InternalOutcome outcome;
        if (settlements.isEmpty()) {
            outcome = new InternalOutcome(txn, OutcomeType.UNMATCHED_INTERNAL, List.of(), null, null);
        } else if (settlements.size() == 1) {
            outcome = classifySingle(txn, settlements.getFirst());
        } else {
            outcome = classifyMultiple(txn, settlements);
        }

        if (txn.type() == TxnType.REFUND && !saleRefs.contains(txn.merchantRef())) {
            return new InternalOutcome(
                    txn,
                    OutcomeType.ORPHAN_REFUND,
                    settlements,
                    "no sale for " + txn.merchantRef(),
                    outcome.settlementDayOffset()
            );
        }
        return outcome;
    }

    private InternalOutcome classifySingle(InternalTransaction txn, SettlementRecord settlement) {
        BigDecimal gross = txn.grossAmount().amount();
        BigDecimal settled = settlement.settledAmount().amount();
        BigDecimal reportedIc = settlement.interchangeFee().amount();
        BigDecimal reportedPf = settlement.processorFee().amount();

        BigDecimal impliedNet = gross.subtract(reportedIc).subtract(reportedPf);
        if (!withinTolerance(settled, impliedNet)) {
            return new InternalOutcome(
                    txn,
                    OutcomeType.AMOUNT_MISMATCH,
                    List.of(settlement),
                    "settled " + settled + " vs implied " + impliedNet,
                    dayOffset(txn, settlement)
            );
        }

        if (!withinTolerance(reportedIc, txn.expectedInterchange().amount())
                || !withinTolerance(reportedPf, txn.expectedProcessor().amount())) {
            return new InternalOutcome(
                    txn,
                    OutcomeType.FEE_DISCREPANCY,
                    List.of(settlement),
                    "reported fees differ from schedule",
                    dayOffset(txn, settlement)
            );
        }

        int offset = dayOffset(txn, settlement);
        if (offset < config.windowMinDays() || offset > config.windowMaxDays()) {
            return new InternalOutcome(
                    txn,
                    OutcomeType.WIDE_WINDOW,
                    List.of(settlement),
                    "T+" + offset,
                    offset
            );
        }

        return new InternalOutcome(txn, OutcomeType.MATCHED, List.of(settlement), null, offset);
    }

    private InternalOutcome classifyMultiple(InternalTransaction txn, List<SettlementRecord> settlements) {
        BigDecimal expectedNet = txn.expectedNet().amount();
        boolean eachRepeatsNet = settlements.stream()
                .allMatch(row -> withinTolerance(row.settledAmount().amount(), expectedNet));

        if (eachRepeatsNet) {
            return new InternalOutcome(
                    txn,
                    OutcomeType.DUPLICATE_SETTLEMENT,
                    settlements,
                    settlements.size() + " rows each at expected net",
                    dayOffset(txn, settlements.getFirst())
            );
        }

        BigDecimal sum = settlements.stream()
                .map(row -> row.settledAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal splitTolerance = config.tolerance().multiply(BigDecimal.valueOf(settlements.size()));
        if (sum.subtract(expectedNet).abs().compareTo(splitTolerance) <= 0) {
            return new InternalOutcome(
                    txn,
                    OutcomeType.SPLIT_SETTLEMENT,
                    settlements,
                    settlements.size() + " rows sum to " + sum,
                    dayOffset(txn, settlements.getFirst())
            );
        }

        return new InternalOutcome(
                txn,
                OutcomeType.AMBIGUOUS,
                settlements,
                settlements.size() + " rows sum to " + sum + ", expected " + expectedNet,
                dayOffset(txn, settlements.getFirst())
        );
    }

    private int dayOffset(InternalTransaction txn, SettlementRecord settlement) {
        LocalDate captured = txn.capturedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return (int) (settlement.settlementDate().toEpochDay() - captured.toEpochDay());
    }

    private boolean withinTolerance(BigDecimal left, BigDecimal right) {
        return left.subtract(right).abs().compareTo(config.tolerance()) <= 0;
    }
}
