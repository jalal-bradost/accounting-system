package com.bradox.erp.sales.dataaccess.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalesNetRevenueSqlTest {

    @Test
    void lineAndOrderFragmentsReferenceReturnsAndToDate() {
        assertThat(SalesNetRevenueSql.NET_QTY_FOR_LINE_L)
                .contains("GREATEST")
                .contains("qty_ordered")
                .contains("INCOMING")
                .contains(":toDate");
        assertThat(SalesNetRevenueSql.LINE_NET_COMPANY_AMOUNT)
                .contains(SalesNetRevenueSql.NET_QTY_FOR_LINE_L)
                .contains("unit_price");
        assertThat(SalesNetRevenueSql.ORDER_NET_COMPANY_AMOUNT)
                .contains("sal_sales_order_line")
                .contains("is_gift")
                .contains("INCOMING")
                .contains(":toDate");
    }
}
