package com.settlement.reconciliation.fee;

import com.settlement.reconciliation.domain.Money;

public record ExpectedFees(
        Money interchangeFee,
        Money processorFee,
        Money expectedNet
) {
}
