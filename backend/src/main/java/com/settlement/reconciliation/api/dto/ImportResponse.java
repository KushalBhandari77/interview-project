package com.settlement.reconciliation.api.dto;

import java.time.Instant;

public record ImportResponse(
        Long batchId,
        Instant importedAt,
        int internalCount,
        int settlementCount,
        int quarantinedCount,
        boolean reusedExisting
) {
}
