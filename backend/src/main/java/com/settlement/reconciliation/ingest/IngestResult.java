package com.settlement.reconciliation.ingest;

import java.util.List;

public record IngestResult(
        List<InternalTransaction> internalTransactions,
        List<SettlementRecord> settlementRecords,
        List<QuarantinedRow> quarantinedRows
) {
}
