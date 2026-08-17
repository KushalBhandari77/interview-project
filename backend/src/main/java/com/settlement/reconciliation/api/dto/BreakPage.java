package com.settlement.reconciliation.api.dto;

import java.util.List;

public record BreakPage(
        List<BreakSummary> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
