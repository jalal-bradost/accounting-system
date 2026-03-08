package com.jalaldeveloper.accountingsystem.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void constructor_scalesAmount() {
        Money m = new Money(new BigDecimal("1.234567"));
        assertThat(m.getAmount().toString()).isEqualTo("1.2346");
    }

    @Test
    void constructor_nullBecomesZero() {
        Money m = new Money(null);
        assertThat(m.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void zero_constant() {
        assertThat(Money.ZERO.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void add_returnsScaledSum() {
        Money a = new Money(new BigDecimal("10.12"));
        Money b = new Money(new BigDecimal("20.34"));
        Money sum = a.add(b);
        assertThat(sum.getAmount()).isEqualByComparingTo(new BigDecimal("30.4600"));
    }

    @Test
    void subtract_returnsScaledDifference() {
        Money a = new Money(new BigDecimal("20.50"));
        Money b = new Money(new BigDecimal("5.25"));
        Money diff = a.subtract(b);
        assertThat(diff.getAmount()).isEqualByComparingTo(new BigDecimal("15.2500"));
    }

    @Test
    void multiply_returnsScaledProduct() {
        Money m = new Money(new BigDecimal("10.1111"));
        Money product = m.multiply(3);
        assertThat(product.getAmount()).isEqualByComparingTo(new BigDecimal("30.3333"));
    }

    @Test
    void equals_and_hashCode() {
        Money a = new Money(new BigDecimal("1.0000"));
        Money b = new Money(new BigDecimal("1.00"));
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isGreaterThanZero() {
        assertThat(Money.ZERO.isGreaterThanZero()).isFalse();
        assertThat(new Money(new BigDecimal("0.0001")).isGreaterThanZero()).isTrue();
        assertThat(new Money(new BigDecimal("-0.0001")).isGreaterThanZero()).isFalse();
    }

    @Test
    void isGreaterThan() {
        Money ten = new Money(new BigDecimal("10"));
        Money five = new Money(new BigDecimal("5"));
        assertThat(ten.isGreaterThan(five)).isTrue();
        assertThat(five.isGreaterThan(ten)).isFalse();
    }
}
