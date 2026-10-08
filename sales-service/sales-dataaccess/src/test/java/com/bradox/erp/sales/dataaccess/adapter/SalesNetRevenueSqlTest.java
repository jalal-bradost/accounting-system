package com.bradox.erp.sales.dataaccess.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalesNetRevenueSqlTest {

    @Test
    void revenueAppliesLineAndOrderDiscountsWithoutSubtractingReturnsTwice() {
        assertThat(SalesNetRevenueSql.LINE_NET_COMPANY_AMOUNT)
                .contains(SalesNetRevenueSql.NET_QTY_FOR_LINE_L)
                .contains("l.discount_percent")
                .contains(SalesNetRevenueSql.ORDER_DISCOUNT_FACTOR_O);
        assertThat(SalesNetRevenueSql.ORDER_NET_COMPANY_AMOUNT)
                .contains("is_gift")
                .contains("l2.discount_percent")
                .contains(SalesNetRevenueSql.ORDER_DISCOUNT_FACTOR_O);
        // Refund returns already lower qty_ordered; exchange returns are delivered again.
        assertThat(SalesNetRevenueSql.LINE_NET_COMPANY_AMOUNT).doesNotContain("INCOMING");
        assertThat(SalesNetRevenueSql.ORDER_NET_COMPANY_AMOUNT).doesNotContain("INCOMING");
    }
}
