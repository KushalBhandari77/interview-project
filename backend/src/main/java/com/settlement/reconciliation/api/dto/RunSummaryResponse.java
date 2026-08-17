package com.settlement.reconciliation.api.dto;

import java.time.Instant;
import java.util.List;

public record RunSummaryResponse(
        Long runId,
        Long batchId,
        Instant runAt,
        long cleanMatchCount,
        List<CategorySummary> categories,
        PayoutSummary payout
) {
}
