package com.bradox.erp.sales.dataaccess.adapter;

import com.bradox.erp.sales.service.domain.ports.output.SalesProductProfitQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SalesProductProfitQueryAdapter implements SalesProductProfitQueryPort {

    private static final String NON_GIFT_LINE =
            "AND COALESCE(l.is_gift, FALSE) = FALSE";

    private static final String CONFIRMED_IN_RANGE =
            "o.state = 'CONFIRMED' AND ("
                    + "(o.order_date IS NOT NULL AND o.order_date >= :fromDate AND o.order_date <= :toDate) "
                    + "OR (o.confirmed_at IS NOT NULL AND CAST(o.confirmed_at AS DATE) >= :fromDate "
                    + "AND CAST(o.confirmed_at AS DATE) <= :toDate))";

    private static final String INVOICE_SIGN =
            "(CASE WHEN i.move_type = 'CREDIT_NOTE' THEN -1 ELSE 1 END)";

    /** Invoice subtotal before the order discount (non-gift lines after line discounts), alias {@code i}. */
    private static final String INVOICE_UNTAXED =
            "(SELECT COALESCE(SUM(x.qty * x.unit_price * (1 - COALESCE(x.discount_percent, 0) / 100)), 0) "
                    + "FROM acc_customer_invoice_line x "
                    + "WHERE x.customer_invoice_id = i.id AND COALESCE(x.is_gift, FALSE) = FALSE)";

    /** Share of each non-gift line taken by the invoice's order discount, as posted to Sales Discount. */
    private static final String ORDER_DISCOUNT_SHARE =
            "(CASE WHEN " + INVOICE_UNTAXED + " > 0 "
                    + "THEN LEAST(COALESCE(i.order_discount_amount, 0), " + INVOICE_UNTAXED + ") / " + INVOICE_UNTAXED + " "
                    + "ELSE 0 END)";

    /**
     * Net revenue an invoice line ({@code il}) posts to INCOME accounts, company currency: gift
     * lines credit revenue at full price; other lines after line and order discounts.
     */
    private static final String INVOICE_LINE_REVENUE =
            "(" + INVOICE_SIGN + " * COALESCE(i.exchange_rate_to_company, 1) * "
                    + "(CASE WHEN COALESCE(il.is_gift, FALSE) = TRUE THEN il.qty * il.unit_price "
                    + "ELSE il.qty * il.unit_price * (1 - COALESCE(il.discount_percent, 0) / 100) "
                    + "* (1 - " + ORDER_DISCOUNT_SHARE + ") END))";

    /** Posted, non-opening invoices dated in the period whose journal entry still stands. */
    private static final String INVOICE_ON_BOOKS =
            "i.state = 'POSTED' "
                    + "AND COALESCE(i.opening_balance, FALSE) = FALSE "
                    + "AND i.invoice_date IS NOT NULL "
                    + "AND i.invoice_date >= :fromDate AND i.invoice_date <= :toDate "
                    + "AND NOT EXISTS (SELECT 1 FROM journal_entries rv "
                    + "WHERE rv.reversal_of_entry_id = i.journal_entry_id)";

    /** Average delivered unit cost per sales order line, as the COGS clearing values it. */
    private static final String LINE_DELIVERED_UNIT_COST =
            "(SELECT m.sales_order_line_id AS line_id, "
                    + "SUM(m.picked_quantity * m.unit_cost) / SUM(m.picked_quantity) AS unit_cost "
                    + "FROM inv_stock_move m "
                    + "JOIN inv_stock_picking pk ON pk.id = m.picking_id "
                    + "WHERE m.state = 'DONE' AND pk.picking_type = 'OUTGOING' "
                    + "AND m.sales_order_line_id IS NOT NULL "
                    + "GROUP BY m.sales_order_line_id "
                    + "HAVING SUM(m.picked_quantity) > 0)";

    /** COGS clearing entries are dated at the order date. */
    private static final String COGS_ORDER_IN_RANGE =
            "COALESCE(o.order_date, CAST(o.confirmed_at AS DATE)) >= :fromDate "
                    + "AND COALESCE(o.order_date, CAST(o.confirmed_at AS DATE)) <= :toDate";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<ConfirmedProductFact> confirmedProductFacts(UUID companyId, java.time.LocalDate from,
                                                            java.time.LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT p.id, p.name, COUNT(DISTINCT o.id), "
                        + "COALESCE(SUM(" + SalesNetRevenueSql.NET_QTY_FOR_LINE_L + "), 0), "
                        + "COALESCE(SUM(" + SalesNetRevenueSql.LINE_NET_COMPANY_AMOUNT + "), 0) "
                        + "FROM sal_sales_order_line l "
                        + "JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "JOIN inv_product p ON p.id = l.product_id "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE + " "
                        + NON_GIFT_LINE + " "
                        + "GROUP BY p.id, p.name "
                        + "HAVING COALESCE(SUM(" + SalesNetRevenueSql.NET_QTY_FOR_LINE_L + "), 0) > 0");
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        List<Object[]> rows = q.getResultList();
        List<ConfirmedProductFact> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new ConfirmedProductFact(
                    toUuid(row[0]),
                    row[1] != null ? row[1].toString() : null,
                    ((Number) row[2]).longValue(),
                    toBigDecimal(row[3]),
                    toBigDecimal(row[4])));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<InvoicedProductFact> invoicedProductFacts(UUID companyId, java.time.LocalDate from,
                                                          java.time.LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT l.product_id, MAX(p.name), "
                        + "COALESCE(SUM(" + INVOICE_SIGN + " * il.qty), 0), "
                        + "COALESCE(SUM(" + INVOICE_LINE_REVENUE + "), 0) "
                        + "FROM acc_customer_invoice_line il "
                        + "JOIN acc_customer_invoice i ON i.id = il.customer_invoice_id "
                        + "LEFT JOIN sal_sales_order_line l ON l.id = il.sales_order_line_id "
                        + "LEFT JOIN inv_product p ON p.id = l.product_id "
                        + "WHERE i.company_id = :companyId "
                        + "AND " + INVOICE_ON_BOOKS + " "
                        + "GROUP BY l.product_id");
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        List<Object[]> rows = q.getResultList();
        List<InvoicedProductFact> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new InvoicedProductFact(
                    toUuid(row[0]),
                    row[1] != null ? row[1].toString() : null,
                    toBigDecimal(row[2]),
                    toBigDecimal(row[3])));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PendingDeliveryLine> pendingDeliveryLines(UUID companyId, java.time.LocalDate from,
                                                          java.time.LocalDate to) {
        // Per sales order line: units invoiced in the period that are still not covered by a posted
        // COGS (COGS posts at min(delivered, invoiced)), valued at the revenue those units billed.
        Query q = entityManager.createNativeQuery(
                "SELECT t.order_id, t.order_name, pt.display_name, t.product_id, "
                        + "LEAST(t.uncleared, t.qty_p), "
                        + "LEAST(t.uncleared, t.qty_p) * t.rev_p / t.qty_p, "
                        + "t.first_invoice_date "
                        + "FROM (SELECT l.id AS line_id, l.product_id AS product_id, "
                        + "o.id AS order_id, o.name AS order_name, "
                        + "o.customer_partner_id AS customer_id, "
                        + "GREATEST(l.qty_invoiced - l.qty_cogs_cleared, 0) AS uncleared, "
                        + "SUM(" + INVOICE_SIGN + " * il.qty) AS qty_p, "
                        + "SUM(" + INVOICE_LINE_REVENUE + ") AS rev_p, "
                        + "MIN(i.invoice_date) AS first_invoice_date "
                        + "FROM acc_customer_invoice_line il "
                        + "JOIN acc_customer_invoice i ON i.id = il.customer_invoice_id "
                        + "JOIN sal_sales_order_line l ON l.id = il.sales_order_line_id "
                        + "JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "JOIN inv_product p ON p.id = l.product_id "
                        + "WHERE i.company_id = :companyId "
                        + "AND " + INVOICE_ON_BOOKS + " "
                        + "AND p.product_type = 'STOCKABLE' "
                        + "AND COALESCE(l.is_gift, FALSE) = FALSE "
                        + "GROUP BY l.id, l.product_id, o.id, o.name, o.customer_partner_id, "
                        + "l.qty_invoiced, l.qty_cogs_cleared) t "
                        + "LEFT JOIN contacts_partner pt ON pt.id = t.customer_id "
                        + "WHERE t.qty_p > 0 AND t.uncleared > 0");
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        List<Object[]> rows = q.getResultList();
        List<PendingDeliveryLine> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new PendingDeliveryLine(
                    toUuid(row[0]),
                    row[1] != null ? row[1].toString() : null,
                    row[2] != null ? row[2].toString() : null,
                    toUuid(row[3]),
                    toBigDecimal(row[4]),
                    toBigDecimal(row[5]),
                    row[6] != null ? java.sql.Date.valueOf(row[6].toString().substring(0, 10)).toLocalDate() : null));
        }
        return result;
    }

    @Override
    public PipelineFact quotationPipeline(UUID companyId, java.time.LocalDate from, java.time.LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COUNT(*), COALESCE(SUM(o.amount_untaxed * COALESCE(o.exchange_rate_to_company, 1)), 0) "
                        + "FROM sal_sales_order o "
                        + "WHERE o.company_id = :companyId "
                        + "AND o.state IN ('DRAFT', 'QUOTATION_SENT') "
                        + "AND o.order_date IS NOT NULL AND o.order_date >= :fromDate AND o.order_date <= :toDate");
        return pipeline(q, companyId, from, to);
    }

    @Override
    public PipelineFact confirmedNotInvoiced(UUID companyId, java.time.LocalDate from, java.time.LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COUNT(DISTINCT CASE WHEN l.qty_ordered > l.qty_invoiced THEN o.id END), "
                        + "COALESCE(SUM(GREATEST(l.qty_ordered - l.qty_invoiced, 0) * l.unit_price "
                        + "* (1 - COALESCE(l.discount_percent, 0) / 100) "
                        + "* " + SalesNetRevenueSql.ORDER_DISCOUNT_FACTOR_O + " "
                        + "* COALESCE(o.exchange_rate_to_company, 1)), 0) "
                        + "FROM sal_sales_order_line l "
                        + "JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE + " "
                        + NON_GIFT_LINE);
        return pipeline(q, companyId, from, to);
    }

    private PipelineFact pipeline(Query q, UUID companyId, java.time.LocalDate from, java.time.LocalDate to) {
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        Object[] row = (Object[]) q.getSingleResult();
        return new PipelineFact(((Number) row[0]).longValue(), toBigDecimal(row[1]));
    }

    @Override
    public List<CostFact> costOfSalesByProduct(UUID companyId, java.time.LocalDate from, java.time.LocalDate to) {
        return costOfSales(companyId, from, to, "l.product_id");
    }

    @SuppressWarnings("unchecked")
    private List<CostFact> costOfSales(UUID companyId, java.time.LocalDate from, java.time.LocalDate to,
                                       String groupColumn) {
        Query q = entityManager.createNativeQuery(
                "SELECT " + groupColumn + ", COALESCE(SUM(l.qty_cogs_cleared * c.unit_cost), 0) "
                        + "FROM sal_sales_order_line l "
                        + "JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "JOIN " + LINE_DELIVERED_UNIT_COST + " c ON c.line_id = l.id "
                        + "WHERE o.company_id = :companyId "
                        + "AND l.qty_cogs_cleared <> 0 "
                        + "AND " + COGS_ORDER_IN_RANGE + " "
                        + "GROUP BY " + groupColumn);
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        List<Object[]> rows = q.getResultList();
        List<CostFact> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new CostFact(toUuid(row[0]), toBigDecimal(row[1])));
        }
        return result;
    }

    @Override
    public LedgerProfitTotals ledgerProfitTotals(UUID companyId, java.time.LocalDate from, java.time.LocalDate to) {
        // Same journal lines as the Profit & Loss: posted entries, entry date in [from, to + 1 day).
        Query q = entityManager.createNativeQuery(
                "SELECT "
                        + "COALESCE(SUM(CASE WHEN a.type = 'INCOME' THEN ji.credit - ji.debit ELSE 0 END), 0), "
                        + "COALESCE(SUM(CASE WHEN a.type = 'COST_OF_REVENUE' THEN ji.debit - ji.credit ELSE 0 END), 0) "
                        + "FROM journal_items ji "
                        + "JOIN journal_entries e ON e.id = ji.journal_entry_id "
                        + "JOIN accounts a ON a.id = ji.account_id "
                        + "WHERE e.company_id = :companyId AND e.status = 'POSTED' "
                        + "AND a.type IN ('INCOME', 'COST_OF_REVENUE') "
                        + "AND e.entry_date >= :fromInclusive AND e.entry_date < :toExclusive");
        q.setParameter("companyId", companyId);
        q.setParameter("fromInclusive", from.atStartOfDay());
        q.setParameter("toExclusive", to.plusDays(1).atStartOfDay());
        Object[] row = (Object[]) q.getSingleResult();
        return new LedgerProfitTotals(toBigDecimal(row[0]), toBigDecimal(row[1]));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<UUID, BigDecimal> currentValuationUnitCosts(UUID companyId, Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Query q = entityManager.createNativeQuery(
                "SELECT p.id, p.standard_cost, p.product_type, "
                        + "COALESCE((SELECT SUM(q.quantity) FROM inv_stock_quant q "
                        + "JOIN inv_stock_location loc ON loc.id = q.location_id "
                        + "WHERE q.company_id = :companyId AND q.product_id = p.id "
                        + "AND loc.location_type = 'INTERNAL'), 0), "
                        + "COALESCE((SELECT SUM(svl.total_value) FROM inv_stock_valuation_layer svl "
                        + "WHERE svl.company_id = :companyId AND svl.product_id = p.id), 0) "
                        + "FROM inv_product p "
                        + "WHERE p.company_id = :companyId AND p.id IN :productIds");
        q.setParameter("companyId", companyId);
        q.setParameter("productIds", List.copyOf(productIds));
        List<Object[]> rows = q.getResultList();
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) {
            UUID id = toUuid(row[0]);
            BigDecimal standardCost = toBigDecimal(row[1]);
            String productType = row[2] != null ? row[2].toString() : "STOCKABLE";
            BigDecimal onHand = toBigDecimal(row[3]);
            BigDecimal valuation = toBigDecimal(row[4]);
            if (!"STOCKABLE".equals(productType)) {
                result.put(id, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                continue;
            }
            BigDecimal unitCost;
            if (onHand.signum() > 0) {
                unitCost = valuation.divide(onHand, 8, RoundingMode.HALF_UP);
            } else {
                unitCost = standardCost;
            }
            if (unitCost.signum() < 0) {
                unitCost = BigDecimal.ZERO;
            }
            result.put(id, unitCost.setScale(4, RoundingMode.HALF_UP));
        }
        return result;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
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
