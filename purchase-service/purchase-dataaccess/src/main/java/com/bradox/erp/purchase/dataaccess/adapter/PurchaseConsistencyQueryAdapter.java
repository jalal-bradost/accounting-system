package com.bradox.erp.purchase.dataaccess.adapter;

import com.bradox.erp.purchase.service.domain.ports.output.PurchaseConsistencyQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class PurchaseConsistencyQueryAdapter implements PurchaseConsistencyQueryPort {

    /** Quantities are stored with 4 decimals; anything smaller is rounding noise. */
    private static final String EPS = "0.0001";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Issue> linesOrderedBelowReceivedOrBilled(UUID companyId, UUID purchaseOrderId) {
        return run("ordered_below_received_or_billed",
                "SELECT o.id, o.name, l.name, l.qty_ordered, l.qty_received, l.qty_invoiced "
                        + "FROM pur_purchase_order_line l JOIN pur_purchase_order o ON o.id = l.purchase_order_id "
                        + "WHERE o.company_id = :companyId AND o.state = 'CONFIRMED' "
                        + "AND (l.qty_ordered + " + EPS + " < l.qty_received OR l.qty_ordered + " + EPS + " < l.qty_invoiced)",
                companyId, purchaseOrderId,
                r -> r[2] + ": ordered " + num(r[3]) + ", received " + num(r[4]) + ", billed " + num(r[5]));
    }

    @Override
    public List<Issue> linesBilledQtyMismatch(UUID companyId, UUID purchaseOrderId) {
        return run("billed_qty_mismatch",
                "SELECT o.id, o.name, l.name, l.qty_invoiced, COALESCE(d.net_qty, 0) "
                        + "FROM pur_purchase_order_line l JOIN pur_purchase_order o ON o.id = l.purchase_order_id "
                        + "LEFT JOIN (SELECT bl.purchase_order_line_id AS pol_id, "
                        + "  SUM(CASE WHEN b.move_type = 'CREDIT_NOTE' THEN -bl.qty ELSE bl.qty END) AS net_qty "
                        + "  FROM pur_vendor_bill_line bl JOIN pur_vendor_bill b ON b.id = bl.vendor_bill_id "
                        + "  WHERE b.state = 'POSTED' AND COALESCE(b.opening_balance, FALSE) = FALSE "
                        + "  AND b.move_type IN ('BILL', 'CREDIT_NOTE') "
                        + "  AND bl.purchase_order_line_id IS NOT NULL GROUP BY bl.purchase_order_line_id) d ON d.pol_id = l.id "
                        + "WHERE o.company_id = :companyId "
                        + "AND ABS(l.qty_invoiced - GREATEST(COALESCE(d.net_qty, 0), 0)) > " + EPS,
                companyId, purchaseOrderId,
                r -> r[2] + ": order line says " + num(r[3]) + ", posted documents say " + num(r[4]));
    }

    @Override
    public List<Issue> linesOnSeveralOpenReceipts(UUID companyId, UUID purchaseOrderId) {
        return run("several_open_receipts",
                "SELECT o.id, o.name, l.name, COUNT(DISTINCT p.id) "
                        + "FROM pur_purchase_order_line l JOIN pur_purchase_order o ON o.id = l.purchase_order_id "
                        + "JOIN inv_stock_move m ON m.purchase_order_line_id = l.id "
                        + "JOIN inv_stock_picking p ON p.id = m.picking_id "
                        + "WHERE o.company_id = :companyId AND p.picking_type = 'INCOMING' "
                        + "AND p.state NOT IN ('DONE', 'CANCELLED') AND m.state NOT IN ('DONE', 'CANCELLED') "
                        + "{ORDER_FILTER} "
                        + "GROUP BY o.id, o.name, l.id, l.name HAVING COUNT(DISTINCT p.id) > 1",
                companyId, purchaseOrderId,
                r -> r[2] + ": on " + num(r[3]) + " open receipts");
    }

    @Override
    public List<Issue> postedDocumentsWithReversedEntry(UUID companyId, UUID purchaseOrderId) {
        return run("posted_document_entry_reversed",
                "SELECT o.id, o.name, b.reference, r.sequence_number "
                        + "FROM pur_vendor_bill b JOIN pur_purchase_order o ON o.id = b.purchase_order_id "
                        + "JOIN journal_entries r ON r.reversal_of_entry_id = b.journal_entry_id "
                        + "WHERE o.company_id = :companyId AND b.state = 'POSTED' AND r.status = 'POSTED'",
                companyId, purchaseOrderId,
                r -> r[2] + " is posted but its journal entry was reversed by " + r[3]);
    }

    @Override
    public List<Issue> cancelledOrdersWithActivity(UUID companyId, UUID purchaseOrderId) {
        return run("cancelled_order_with_activity",
                "SELECT o.id, o.name, l.name, l.qty_received, l.qty_invoiced "
                        + "FROM pur_purchase_order_line l JOIN pur_purchase_order o ON o.id = l.purchase_order_id "
                        + "WHERE o.company_id = :companyId AND o.state = 'CANCELLED' "
                        + "AND (l.qty_received > " + EPS + " OR l.qty_invoiced > " + EPS + ")",
                companyId, purchaseOrderId,
                r -> r[2] + ": received " + num(r[3]) + ", billed " + num(r[4]));
    }

    /**
     * Runs a check query whose first two columns are the order id and name; {@code detail} turns a
     * row into a readable description. The optional order filter is appended to the WHERE clause,
     * or placed at {ORDER_FILTER} when the query groups.
     */
    @SuppressWarnings("unchecked")
    private List<Issue> run(String check, String sql, UUID companyId, UUID purchaseOrderId,
                            Function<Object[], String> detail) {
        String filter = purchaseOrderId != null ? "AND o.id = :purchaseOrderId" : "";
        String finalSql = sql.contains("{ORDER_FILTER}")
                ? sql.replace("{ORDER_FILTER}", filter)
                : sql + " " + filter;
        Query q = entityManager.createNativeQuery(finalSql);
        q.setParameter("companyId", companyId);
        if (purchaseOrderId != null) {
            q.setParameter("purchaseOrderId", purchaseOrderId);
        }
        List<Issue> issues = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            issues.add(new Issue(check, toUuid(row[0]), row[1] != null ? row[1].toString() : null, detail.apply(row)));
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
