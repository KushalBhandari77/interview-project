package com.settlement.reconciliation.api.dto;

import java.time.Instant;

public record RunListItem(
        Long runId,
        Long batchId,
        Instant runAt
) {
}
