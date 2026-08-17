package com.settlement.reconciliation.fee;

import com.settlement.reconciliation.domain.CardType;
import com.settlement.reconciliation.domain.Money;
import com.settlement.reconciliation.domain.TxnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeCalculatorTest {

    private FeeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FeeCalculator(FeeScheduleLoader.loadFromClasspath());
    }

    @Test
    void visaSaleFees() {
        ExpectedFees fees = calculator.calculate(TxnType.SALE, CardType.VISA, Money.of("100.00"));

        assertEquals(Money.of("1.90"), fees.interchangeFee());
        assertEquals(Money.of("0.35"), fees.processorFee());
        assertEquals(Money.of("97.75"), fees.expectedNet());
    }

    @Test
    void mastercardSaleFees() {
        ExpectedFees fees = calculator.calculate(TxnType.SALE, CardType.MASTERCARD, Money.of("100.00"));

        assertEquals(Money.of("2.00"), fees.interchangeFee());
        assertEquals(Money.of("0.35"), fees.processorFee());
        assertEquals(Money.of("97.65"), fees.expectedNet());
    }

    @Test
    void amexSaleFees() {
        ExpectedFees fees = calculator.calculate(TxnType.SALE, CardType.AMEX, Money.of("100.00"));

        assertEquals(Money.of("2.65"), fees.interchangeFee());
        assertEquals(Money.of("0.35"), fees.processorFee());
        assertEquals(Money.of("97.00"), fees.expectedNet());
    }

    @Test
    void discoverSaleFees() {
        ExpectedFees fees = calculator.calculate(TxnType.SALE, CardType.DISCOVER, Money.of("100.00"));

        assertEquals(Money.of("2.10"), fees.interchangeFee());
        assertEquals(Money.of("0.35"), fees.processorFee());
        assertEquals(Money.of("97.55"), fees.expectedNet());
    }

    @Test
    void matchesTestDatasetVisaSale() {
        // TXN-000010 from test/ — amount mismatch row, expected net is 741.75
        ExpectedFees fees = calculator.calculate(TxnType.SALE, CardType.VISA, Money.of("757.81"));

        assertEquals(Money.of("13.74"), fees.interchangeFee());
        assertEquals(Money.of("2.32"), fees.processorFee());
        assertEquals(Money.of("741.75"), fees.expectedNet());
    }

    @Test
    void roundsEachFeeHalfUpBeforeNet() {
        // gross where VISA interchange lands on .005 — must round up to 1.01, not 1.00
        Money gross = Money.of("50.277777777777777778");
        ExpectedFees fees = calculator.calculate(TxnType.SALE, CardType.VISA, gross);

        assertEquals(Money.of("1.01"), fees.interchangeFee());
        assertEquals(new BigDecimal("1.005").setScale(2, RoundingMode.HALF_UP), fees.interchangeFee().amount());
    }

    @Test
    void refundHasNoFees() {
        Money gross = Money.of("-336.42");
        ExpectedFees fees = calculator.calculate(TxnType.REFUND, CardType.MASTERCARD, gross);

        assertEquals(Money.zero(), fees.interchangeFee());
        assertEquals(Money.zero(), fees.processorFee());
        assertEquals(gross, fees.expectedNet());
    }
}
