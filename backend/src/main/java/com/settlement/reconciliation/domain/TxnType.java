package com.settlement.reconciliation.domain;

public enum TxnType {
    SALE,
    REFUND;

    public static TxnType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("transaction type is required");
        }
        return TxnType.valueOf(value.trim().toUpperCase());
    }
}
