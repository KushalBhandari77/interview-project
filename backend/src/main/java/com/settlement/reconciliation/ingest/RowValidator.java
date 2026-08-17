package com.settlement.reconciliation.ingest;

import com.settlement.reconciliation.domain.CardType;
import com.settlement.reconciliation.domain.TxnType;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class RowValidator {

    private static final Set<String> KNOWN_CARDS = Set.of("VISA", "MASTERCARD", "AMEX", "DISCOVER");

    public Optional<RejectReason> validateInternal(InternalRow row) {
        if (isBlank(row.internalTxnId()) || isBlank(row.merchantId()) || isBlank(row.merchantRef())) {
            return Optional.of(RejectReason.MISSING_FIELD);
        }
        if (isBlank(row.cardType())) {
            return Optional.of(RejectReason.MISSING_CARD_TYPE);
        }
        if (!KNOWN_CARDS.contains(row.cardType().trim().toUpperCase(Locale.US))) {
            return Optional.of(RejectReason.UNKNOWN_CARD_TYPE);
        }
        if (parseAmount(row.grossAmount()) == null) {
            return Optional.of(RejectReason.NON_NUMERIC_AMOUNT);
        }
        if (!"USD".equalsIgnoreCase(trim(row.currency()))) {
            return Optional.of(RejectReason.UNSUPPORTED_CURRENCY);
        }
        TxnType type = parseTxnType(row.type());
        if (type == null) {
            return Optional.of(RejectReason.UNKNOWN_TYPE);
        }
        BigDecimal gross = parseAmount(row.grossAmount());
        if (type == TxnType.SALE && gross.signum() <= 0) {
            return Optional.of(RejectReason.SIGN_MISMATCH);
        }
        if (type == TxnType.REFUND && gross.signum() >= 0) {
            return Optional.of(RejectReason.SIGN_MISMATCH);
        }
        return Optional.empty();
    }

    public Optional<RejectReason> validateSettlement(SettlementRow row) {
        if (isBlank(row.networkRef())) {
            return Optional.of(RejectReason.MISSING_FIELD);
        }
        if (isBlank(row.settledAmount())) {
            return Optional.of(RejectReason.MISSING_SETTLED_AMOUNT);
        }
        if (parseAmount(row.settledAmount()) == null) {
            return Optional.of(RejectReason.NON_NUMERIC_AMOUNT);
        }
        if (!"USD".equalsIgnoreCase(trim(row.currency()))) {
            return Optional.of(RejectReason.UNSUPPORTED_CURRENCY);
        }
        if (isBlank(row.cardType())) {
            return Optional.of(RejectReason.MISSING_CARD_TYPE);
        }
        if (!KNOWN_CARDS.contains(row.cardType().trim().toUpperCase(Locale.US))) {
            return Optional.of(RejectReason.UNKNOWN_CARD_TYPE);
        }
        if (parseAmount(row.interchangeFee()) == null || parseAmount(row.processorFee()) == null) {
            return Optional.of(RejectReason.NON_NUMERIC_AMOUNT);
        }
        return Optional.empty();
    }

    public CardType parseCard(String value) {
        return CardType.parse(value);
    }

    public TxnType parseTxnType(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.US);
        if (!normalized.equals("SALE") && !normalized.equals("REFUND")) {
            return null;
        }
        return TxnType.valueOf(normalized);
    }

    private BigDecimal parseAmount(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
