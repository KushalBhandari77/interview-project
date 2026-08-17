package com.settlement.reconciliation.ingest;

import com.settlement.reconciliation.domain.CardType;
import com.settlement.reconciliation.domain.Money;
import com.settlement.reconciliation.domain.TxnType;

import java.time.Instant;

public record InternalTransaction(
        String internalTxnId,
        String merchantId,
        String merchantRef,
        CardType cardType,
        String cardLast4,
        Money grossAmount,
        TxnType type,
        Instant capturedAt,
        Money expectedInterchange,
        Money expectedProcessor,
        Money expectedNet
) {
}
