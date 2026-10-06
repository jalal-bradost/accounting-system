package com.bradox.erp.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MonetaryScaleTest {

    @Test
    void scale_returnsZeroScaledWhenNull() {
        BigDecimal result = MonetaryScale.scale(null);
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.scale()).isEqualTo(MonetaryScale.SCALE);
    }

    @Test
    void scale_appliesScaleFourAndHalfEven() {
        // HALF_EVEN: 10.12346 rounds up to 10.1235; 10.12344 rounds down to 10.1234
        assertThat(MonetaryScale.scale(new BigDecimal("10.12346")).toString()).isEqualTo("10.1235");
        assertThat(MonetaryScale.scale(new BigDecimal("10.12344")).toString()).isEqualTo("10.1234");
        assertThat(MonetaryScale.scale(new BigDecimal("10")).toString()).isEqualTo("10.0000");
    }

    @Test
    void scale_zeroRemainsZero() {
        BigDecimal result = MonetaryScale.scale(BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.scale()).isEqualTo(MonetaryScale.SCALE);
    }

    @Test
    void toDisplayString_stripsTrailingZerosButKeepsSignificantFraction() {
        assertThat(MonetaryScale.toDisplayString(new BigDecimal("85000.0000"))).isEqualTo("85000");
        assertThat(MonetaryScale.toDisplayString(new BigDecimal("85000.5990"))).isEqualTo("85000.599");
        assertThat(MonetaryScale.toDisplayString(new BigDecimal("2.5000"))).isEqualTo("2.5");
        assertThat(MonetaryScale.toDisplayString(null)).isEqualTo("0");
        assertThat(MonetaryScale.toDisplayString(BigDecimal.ZERO)).isEqualTo("0");
    }
}
