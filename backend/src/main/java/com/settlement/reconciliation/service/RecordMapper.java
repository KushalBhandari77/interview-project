package com.settlement.reconciliation.service;

import com.settlement.reconciliation.domain.CardType;
import com.settlement.reconciliation.domain.Money;
import com.settlement.reconciliation.domain.TxnType;
import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.SettlementRecord;
import com.settlement.reconciliation.persistence.InternalTxnEntity;
import com.settlement.reconciliation.persistence.SettlementEntity;

import java.util.List;

final class RecordMapper {

    private RecordMapper() {
    }

    static InternalTransaction toInternal(InternalTxnEntity entity) {
        return new InternalTransaction(
                entity.getInternalTxnId(),
                entity.getMerchantId(),
                entity.getMerchantRef(),
                CardType.parse(entity.getCardType()),
                entity.getCardLast4(),
                Money.of(entity.getGrossAmount()),
                TxnType.parse(entity.getTxnType()),
                entity.getCapturedAt(),
                Money.of(entity.getExpectedInterchange()),
                Money.of(entity.getExpectedProcessor()),
                Money.of(entity.getExpectedNet())
        );
    }

    static SettlementRecord toSettlement(SettlementEntity entity) {
        return new SettlementRecord(
                entity.getNetworkRef(),
                entity.getMerchantRef(),
                entity.getMerchantId(),
                CardType.parse(entity.getCardType()),
                entity.getCardLast4(),
                Money.of(entity.getSettledAmount()),
                Money.of(entity.getInterchangeFee()),
                Money.of(entity.getProcessorFee()),
                entity.getSettlementDate()
        );
    }

    static List<InternalTransaction> toInternals(List<InternalTxnEntity> entities) {
        return entities.stream().map(RecordMapper::toInternal).toList();
    }

    static List<SettlementRecord> toSettlements(List<SettlementEntity> entities) {
        return entities.stream().map(RecordMapper::toSettlement).toList();
    }
}
