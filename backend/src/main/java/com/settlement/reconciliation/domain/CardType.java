package com.settlement.reconciliation.domain;

public enum CardType {
    VISA,
    MASTERCARD,
    AMEX,
    DISCOVER;

    public static CardType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("card type is required");
        }
        return CardType.valueOf(value.trim().toUpperCase());
    }
}
