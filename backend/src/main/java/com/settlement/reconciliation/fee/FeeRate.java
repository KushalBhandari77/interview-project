package com.settlement.reconciliation.fee;

import java.math.BigDecimal;

public record FeeRate(BigDecimal percent, BigDecimal flat) {
}
