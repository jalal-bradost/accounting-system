package com.bradox.erp.inventory.domain.core.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPackagingTest {

    @Test
    void toBaseQty_multipliesPackageCountByFactor() {
        assertThat(ProductPackaging.toBaseQty(new BigDecimal("5"), new BigDecimal("20")))
                .isEqualByComparingTo("100");
    }

    @Test
    void fromBaseQty_dividesByFactor() {
        assertThat(ProductPackaging.fromBaseQty(new BigDecimal("100"), new BigDecimal("20")))
                .isEqualByComparingTo("5.0000");
    }

    @Test
    void mixedSale_usesBaseUnits() {
        BigDecimal sets = ProductPackaging.toBaseQty(new BigDecimal("3"), new BigDecimal("20"));
        BigDecimal bottles = ProductPackaging.toBaseQty(new BigDecimal("4"), BigDecimal.ONE);
        assertThat(sets.add(bottles)).isEqualByComparingTo("64");
    }

    @Test
    void normalizeBarcode_blankBecomesNull() {
        assertThat(ProductPackaging.normalizeBarcode("  ")).isNull();
        assertThat(ProductPackaging.normalizeBarcode(" 987 ")).isEqualTo("987");
    }
}
