package com.settlement.reconciliation.engine;

public enum OutcomeType {
    MATCHED,
    UNMATCHED_INTERNAL,
    UNMATCHED_SETTLEMENT,
    AMOUNT_MISMATCH,
    FEE_DISCREPANCY,
    DUPLICATE_SETTLEMENT,
    ORPHAN_REFUND,
    SPLIT_SETTLEMENT,
    WIDE_WINDOW,
    AMBIGUOUS
}
