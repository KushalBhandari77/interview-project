package com.settlement.reconciliation.engine;

import java.math.BigDecimal;

public record ReconConfig(
        BigDecimal tolerance,
        int windowMinDays,
        int windowMaxDays
) {
    public static ReconConfig defaults() {
        return new ReconConfig(new BigDecimal("0.01"), 1, 3);
    }
}
