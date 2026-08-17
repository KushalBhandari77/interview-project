package com.settlement.reconciliation.service;

import java.util.Map;

public record ReconSummary(
        Long runId,
        Long batchId,
        Map<String, Long> outcomeCounts
) {
}
