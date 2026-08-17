package com.settlement.reconciliation.api.dto;

public record QuarantineItem(
        String side,
        int lineNumber,
        String sourceId,
        String reason,
        String rawPayload
) {
}
