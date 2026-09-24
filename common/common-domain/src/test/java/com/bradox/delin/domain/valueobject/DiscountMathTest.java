package com.bradox.delin.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscountMathTest {

    @Test
    void fixedDiscountIsExact() {
        BigDecimal net = DiscountMath.lineNet(
                BigDecimal.ONE, new BigDecimal("8500"), DiscountType.FIXED, new BigDecimal("1000"));
        assertEquals(0, net.compareTo(new BigDecimal("7500")), "net was " + net);
    }

    @Test
    void fixedDiscountNeverGoesNegative() {
        BigDecimal net = DiscountMath.lineNet(
                BigDecimal.ONE, new BigDecimal("100"), DiscountType.FIXED, new BigDecimal("500"));
        assertEquals(0, net.compareTo(BigDecimal.ZERO), "net was " + net);
    }

    @Test
    void percentDiscountClampsToHundred() {
        BigDecimal net = DiscountMath.lineNet(
                new BigDecimal("2"), new BigDecimal("100"), DiscountType.PERCENT, new BigDecimal("150"));
        assertEquals(0, net.compareTo(BigDecimal.ZERO), "net was " + net);
    }

    /** A large fixed discount routed through a 4-decimal percent would drift; the amount must not. */
    @Test
    void largeFixedDiscountDoesNotDrift() {
        BigDecimal net = DiscountMath.lineNet(
                BigDecimal.ONE, new BigDecimal("8500000"), DiscountType.FIXED, new BigDecimal("1000000"));
        assertEquals(0, net.compareTo(new BigDecimal("7500000")), "net was " + net);
    }

    @Test
    void effectivePercentDerivesFromFixedAmount() {
        BigDecimal pct = DiscountMath.effectivePercent(
                new BigDecimal("200"), DiscountType.FIXED, new BigDecimal("50"));
        assertEquals(0, pct.compareTo(new BigDecimal("25")), "pct was " + pct);
    }

    @Test
    void documentLineDiscountSpreadsOrderDiscountAcrossLines() {
        // Two lines of 100 each, order discount of 20 → each line keeps 90.
        BigDecimal amount = DiscountMath.documentLineDiscountAmount(
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("20"));
        assertEquals(0, amount.compareTo(new BigDecimal("10")), "amount was " + amount);
    }

    @Test
    void documentLineDiscountKeepsLineDiscountWhenNoOrderDiscount() {
        BigDecimal amount = DiscountMath.documentLineDiscountAmount(
                new BigDecimal("100"), new BigDecimal("90"), new BigDecimal("90"), BigDecimal.ZERO);
        assertEquals(0, amount.compareTo(new BigDecimal("10")), "amount was " + amount);
    }
}
