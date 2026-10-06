package com.bradox.erp.sales.dataaccess.adapter;

/**
 * Shared SQL fragments for sales revenue net of customer returns.
 * <p>
 * Returns are DONE {@code INCOMING} stock moves linked to a sales order line,
 * counted through {@code :toDate} (inclusive) so period reports stay consistent.
 * Estimated / dashboard “sales revenue” uses ordered qty minus those returns
 * ({@code GREATEST(0, qty_ordered - returned)}), not gross {@code amount_total}.
 */
public final class SalesNetRevenueSql {

    private SalesNetRevenueSql() {}

    /**
     * Correlated return qty for line alias {@code l}. Requires query parameter {@code :toDate}.
     */
    public static final String RETURNED_QTY_FOR_LINE_L =
            "COALESCE((SELECT SUM(rm.picked_quantity) FROM inv_stock_move rm "
                    + "JOIN inv_stock_picking rpk ON rpk.id = rm.picking_id "
                    + "WHERE rm.sales_order_line_id = l.id "
                    + "AND rm.state = 'DONE' AND rpk.state = 'DONE' "
                    + "AND rpk.picking_type = 'INCOMING' "
                    + "AND rpk.validated_at IS NOT NULL "
                    + "AND CAST(rpk.validated_at AS DATE) <= :toDate), 0)";

    /** Net commercial qty for line alias {@code l}. */
    public static final String NET_QTY_FOR_LINE_L =
            "GREATEST(0, l.qty_ordered - (" + RETURNED_QTY_FOR_LINE_L + "))";

    /** Line revenue in company currency for aliases {@code l} + {@code o}. */
    public static final String LINE_NET_COMPANY_AMOUNT =
            "((" + NET_QTY_FOR_LINE_L + ") * l.unit_price "
                    + "* (1 - COALESCE(l.discount_percent, 0) / 100) "
                    + "* COALESCE(o.exchange_rate_to_company, 1))";

    /**
     * Order-level net non-gift revenue for alias {@code o}. Requires {@code :toDate}.
     */
    public static final String ORDER_NET_COMPANY_AMOUNT =
            "COALESCE((SELECT SUM("
                    + "GREATEST(0, l2.qty_ordered - COALESCE(("
                    + "SELECT SUM(rm.picked_quantity) FROM inv_stock_move rm "
                    + "JOIN inv_stock_picking rpk ON rpk.id = rm.picking_id "
                    + "WHERE rm.sales_order_line_id = l2.id "
                    + "AND rm.state = 'DONE' AND rpk.state = 'DONE' "
                    + "AND rpk.picking_type = 'INCOMING' "
                    + "AND rpk.validated_at IS NOT NULL "
                    + "AND CAST(rpk.validated_at AS DATE) <= :toDate"
                    + "), 0)) * l2.unit_price * (1 - COALESCE(l2.discount_percent, 0) / 100) "
                    + "* COALESCE(o.exchange_rate_to_company, 1)"
                    + ") FROM sal_sales_order_line l2 "
                    + "WHERE l2.sales_order_id = o.id AND COALESCE(l2.is_gift, FALSE) = FALSE), 0)";
}
