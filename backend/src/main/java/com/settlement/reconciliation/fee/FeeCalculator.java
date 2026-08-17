package com.settlement.reconciliation.fee;

import com.settlement.reconciliation.domain.CardType;
import com.settlement.reconciliation.domain.Money;
import com.settlement.reconciliation.domain.TxnType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FeeCalculator {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 2;

    private final FeeSchedule schedule;

    public FeeCalculator(FeeSchedule schedule) {
        this.schedule = schedule;
    }

    public ExpectedFees calculate(TxnType type, CardType card, Money gross) {
        if (type == TxnType.REFUND) {
            return new ExpectedFees(Money.zero(), Money.zero(), gross);
        }

        FeeRate interchange = schedule.interchange().get(card);
        if (interchange == null) {
            throw new IllegalArgumentException("no interchange rate for " + card);
        }

        Money interchangeFee = roundFee(gross, interchange);
        Money processorFee = roundFee(gross, schedule.processorMarkup());
        Money expectedNet = gross.subtract(interchangeFee).subtract(processorFee);

        return new ExpectedFees(interchangeFee, processorFee, expectedNet);
    }

    private Money roundFee(Money gross, FeeRate rate) {
        BigDecimal raw = gross.amount()
                .multiply(rate.percent())
                .add(rate.flat());
        return Money.of(raw.setScale(SCALE, ROUNDING));
    }
}
