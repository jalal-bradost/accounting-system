package com.bradox.erp.sales.dataaccess.adapter;

/**
 * Shared SQL fragments for confirmed sales revenue.
 * <p>
 * Revenue uses the order line's current {@code qty_ordered}. A refunded customer return already
 * lowers {@code qty_ordered} (and the order total) when it is validated, and an exchange return is
 * delivered again, so returns must not be subtracted a second time here. Realized figures come from
 * signed stock moves instead.
 */
public final class SalesNetRevenueSql {

    private SalesNetRevenueSql() {}

    /** Commercial qty for line alias {@code l}. */
    public static final String NET_QTY_FOR_LINE_L = "GREATEST(0, l.qty_ordered)";

    /**
     * Share of an order's line subtotal that survives the order-level discount, for alias {@code o}.
     * {@code order_discount_percent} holds the effective percent for both PERCENT and FIXED order
     * discounts (see recalcTotals), so this matches {@code amount_untaxed}.
     */
    public static final String ORDER_DISCOUNT_FACTOR_O =
            "(1 - COALESCE(o.order_discount_percent, 0) / 100)";

    /**
     * Line revenue in company currency for aliases {@code l} + {@code o}, after the line discount
     * ({@code discount_percent} is the effective percent for FIXED discounts too) and the order discount.
     */
    public static final String LINE_NET_COMPANY_AMOUNT =
            "((" + NET_QTY_FOR_LINE_L + ") * l.unit_price "
                    + "* (1 - COALESCE(l.discount_percent, 0) / 100) "
                    + "* " + ORDER_DISCOUNT_FACTOR_O + " "
                    + "* COALESCE(o.exchange_rate_to_company, 1))";

    /** Order-level non-gift revenue in company currency for alias {@code o}. */
    public static final String ORDER_NET_COMPANY_AMOUNT =
            "COALESCE((SELECT SUM("
                    + "GREATEST(0, l2.qty_ordered) * l2.unit_price * (1 - COALESCE(l2.discount_percent, 0) / 100) "
                    + "* " + ORDER_DISCOUNT_FACTOR_O + " "
                    + "* COALESCE(o.exchange_rate_to_company, 1)"
                    + ") FROM sal_sales_order_line l2 "
                    + "WHERE l2.sales_order_id = o.id AND COALESCE(l2.is_gift, FALSE) = FALSE), 0)";
}
