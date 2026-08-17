package com.settlement.reconciliation.ingest;

public enum RejectReason {
    MISSING_FIELD,
    MISSING_CARD_TYPE,
    UNKNOWN_CARD_TYPE,
    NON_NUMERIC_AMOUNT,
    UNSUPPORTED_CURRENCY,
    UNKNOWN_TYPE,
    SIGN_MISMATCH,
    MISSING_SETTLED_AMOUNT
}
