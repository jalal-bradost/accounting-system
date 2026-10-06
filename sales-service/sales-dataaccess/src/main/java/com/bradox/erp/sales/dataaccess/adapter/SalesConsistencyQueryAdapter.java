package com.bradox.erp.sales.dataaccess.adapter;

import com.bradox.erp.sales.service.domain.ports.output.SalesConsistencyQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SalesConsistencyQueryAdapter implements SalesConsistencyQueryPort {

    /** Quantities are stored with 4 decimals; anything smaller is rounding noise. */
    private static final String EPS = "0.0001";

    /** Posted invoice minus credit note quantity per order line, split into charge and gift. */
    private static final String POSTED_NETS =
            "SELECT il.sales_order_line_id AS sol_id, "
                    + "SUM(CASE WHEN COALESCE(il.is_gift, FALSE) = FALSE THEN "
                    + "  (CASE WHEN i.move_type = 'CREDIT_NOTE' THEN -il.qty ELSE il.qty END) ELSE 0 END) AS charge_net, "
                    + "SUM(CASE WHEN COALESCE(il.is_gift, FALSE) = TRUE THEN "
                    + "  (CASE WHEN i.move_type = 'CREDIT_NOTE' THEN -il.qty ELSE il.qty END) ELSE 0 END) AS gift_net "
                    + "FROM acc_customer_invoice_line il JOIN acc_customer_invoice i ON i.id = il.customer_invoice_id "
                    + "WHERE i.state = 'POSTED' AND COALESCE(i.opening_balance, FALSE) = FALSE "
                    + "AND il.sales_order_line_id IS NOT NULL GROUP BY il.sales_order_line_id";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Issue> linesOrderedBelowDeliveredOrInvoiced(UUID companyId, UUID salesOrderId) {
        // Invoiced counts as the larger of the gift and charge nets: a line invoiced as a gift and
        // then re-invoiced as a sale is invoiced once, not twice.
        return run("ordered_below_delivered_or_invoiced",
                "SELECT o.id, o.name, l.name, l.qty_ordered, l.qty_delivered, "
                        + "GREATEST(COALESCE(d.charge_net, 0), COALESCE(d.gift_net, 0)) "
                        + "FROM sal_sales_order_line l JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "LEFT JOIN (" + POSTED_NETS + ") d ON d.sol_id = l.id "
                        + "WHERE o.company_id = :companyId AND o.state = 'CONFIRMED' "
                        + "AND (l.qty_ordered + " + EPS + " < l.qty_delivered "
                        + "OR l.qty_ordered + " + EPS + " < GREATEST(COALESCE(d.charge_net, 0), COALESCE(d.gift_net, 0)))",
                companyId, salesOrderId,
                r -> r[2] + ": ordered " + num(r[3]) + ", delivered " + num(r[4]) + ", invoiced " + num(r[5]));
    }

    @Override
    public List<Issue> linesInvoicedQtyMismatch(UUID companyId, UUID salesOrderId) {
        return run("invoiced_qty_mismatch",
                "SELECT o.id, o.name, l.name, l.qty_invoiced, COALESCE(d.net_qty, 0) "
                        + "FROM sal_sales_order_line l JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "LEFT JOIN (SELECT il.sales_order_line_id AS sol_id, "
                        + "  SUM(CASE WHEN i.move_type = 'CREDIT_NOTE' THEN -il.qty ELSE il.qty END) AS net_qty "
                        + "  FROM acc_customer_invoice_line il JOIN acc_customer_invoice i ON i.id = il.customer_invoice_id "
                        + "  WHERE i.state = 'POSTED' AND COALESCE(i.opening_balance, FALSE) = FALSE "
                        + "  AND il.sales_order_line_id IS NOT NULL GROUP BY il.sales_order_line_id) d ON d.sol_id = l.id "
                        + "WHERE o.company_id = :companyId "
                        + "AND ABS(l.qty_invoiced - GREATEST(COALESCE(d.net_qty, 0), 0)) > " + EPS,
                companyId, salesOrderId,
                r -> r[2] + ": order line says " + num(r[3]) + ", posted documents say " + num(r[4]));
    }

    @Override
    public List<Issue> linesOnSeveralOpenDeliveries(UUID companyId, UUID salesOrderId) {
        return run("several_open_deliveries",
                "SELECT o.id, o.name, l.name, COUNT(DISTINCT p.id) "
                        + "FROM sal_sales_order_line l JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "JOIN inv_stock_move m ON m.sales_order_line_id = l.id "
                        + "JOIN inv_stock_picking p ON p.id = m.picking_id "
                        + "WHERE o.company_id = :companyId AND p.picking_type = 'OUTGOING' "
                        + "AND p.state NOT IN ('DONE', 'CANCELLED') AND m.state NOT IN ('DONE', 'CANCELLED') "
                        + "{ORDER_FILTER} "
                        + "GROUP BY o.id, o.name, l.id, l.name HAVING COUNT(DISTINCT p.id) > 1",
                companyId, salesOrderId,
                r -> r[2] + ": on " + num(r[3]) + " open deliveries");
    }

    @Override
    public List<Issue> postedDocumentsWithReversedEntry(UUID companyId, UUID salesOrderId) {
        return run("posted_document_entry_reversed",
                "SELECT o.id, o.name, i.reference, r.sequence_number "
                        + "FROM acc_customer_invoice i JOIN sal_sales_order o ON o.id = i.sales_order_id "
                        + "JOIN journal_entries r ON r.reversal_of_entry_id = i.journal_entry_id "
                        + "WHERE o.company_id = :companyId AND i.state = 'POSTED' AND r.status = 'POSTED'",
                companyId, salesOrderId,
                r -> r[2] + " is posted but its journal entry was reversed by " + r[3]);
    }

    @Override
    public List<Issue> cancelledOrdersWithActivity(UUID companyId, UUID salesOrderId) {
        return run("cancelled_order_with_activity",
                "SELECT o.id, o.name, l.name, l.qty_delivered, l.qty_invoiced "
                        + "FROM sal_sales_order_line l JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "WHERE o.company_id = :companyId AND o.state = 'CANCELLED' "
                        + "AND (l.qty_delivered > " + EPS + " OR l.qty_invoiced > " + EPS + ")",
                companyId, salesOrderId,
                r -> r[2] + ": delivered " + num(r[3]) + ", invoiced " + num(r[4]));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Issue> unbalancedLedger(UUID companyId) {
        Query q = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(ji.debit), 0) - COALESCE(SUM(ji.credit), 0) "
                        + "FROM journal_items ji JOIN journal_entries je ON je.id = ji.journal_entry_id "
                        + "WHERE je.company_id = :companyId AND je.status = 'POSTED'");
        q.setParameter("companyId", companyId);
        Object diff = q.getSingleResult();
        java.math.BigDecimal d = diff == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(diff.toString());
        if (d.abs().compareTo(new java.math.BigDecimal(EPS)) <= 0) {
            return List.of();
        }
        return List.of(new Issue("unbalanced_ledger", null, null, "debits minus credits = " + d.toPlainString()));
    }

    /**
     * Runs a check query whose first two columns are the order id and name; {@code detail} turns a
     * row into a readable description. The optional order filter is
     * appended to the WHERE clause, or placed at {ORDER_FILTER} when the query groups.
     */
    @SuppressWarnings("unchecked")
    private List<Issue> run(String check, String sql, UUID companyId, UUID salesOrderId,
                            java.util.function.Function<Object[], String> detail) {
        String filter = salesOrderId != null ? "AND o.id = :salesOrderId" : "";
        String finalSql = sql.contains("{ORDER_FILTER}")
                ? sql.replace("{ORDER_FILTER}", filter)
                : sql + " " + filter;
        Query q = entityManager.createNativeQuery(finalSql);
        q.setParameter("companyId", companyId);
        if (salesOrderId != null) {
            q.setParameter("salesOrderId", salesOrderId);
        }
        List<Issue> issues = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            issues.add(new Issue(check, toUuid(row[0]), row[1] != null ? row[1].toString() : null,
                    detail.apply(row)));
        }
        return issues;
    }

    private static String num(Object value) {
        if (value == null) {
            return "0";
        }
        return new java.math.BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
    }

    private static UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }
}
