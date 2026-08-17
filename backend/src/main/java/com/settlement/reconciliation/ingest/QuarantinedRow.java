package com.settlement.reconciliation.ingest;

public record QuarantinedRow(
        RecordSide side,
        int lineNumber,
        String sourceId,
        String rawPayload,
        RejectReason reason
) {
}
