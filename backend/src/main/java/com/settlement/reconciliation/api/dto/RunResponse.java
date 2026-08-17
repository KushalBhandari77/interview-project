package com.settlement.reconciliation.api.dto;

import java.time.Instant;

public record RunResponse(
        Long runId,
        Long batchId,
        Instant runAt
) {
}
