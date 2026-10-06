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

    private static final String MOVE_SIGNED_QTY =
            "(CASE WHEN pk.picking_type = 'OUTGOING' THEN m.picked_quantity "
                    + "WHEN pk.picking_type = 'INCOMING' THEN -m.picked_quantity "
                    + "ELSE 0 END)";

    private static final String MOVE_SIGNED_COST =
            "(CASE WHEN pk.picking_type = 'OUTGOING' THEN m.picked_quantity * m.unit_cost "
                    + "WHEN pk.picking_type = 'INCOMING' THEN -(m.picked_quantity * m.unit_cost) "
                    + "ELSE 0 END)";

    private static final String GROSS_LINE_COMPANY_AMOUNT =
            "((l.qty_ordered * l.unit_price * (1 - COALESCE(l.discount_percent, 0) / 100))"
                    + " * COALESCE(o.exchange_rate_to_company, 1))";

    private static final String MOVE_SIGNED_REVENUE =
            "(CASE WHEN l.qty_ordered IS NULL OR l.qty_ordered = 0 THEN 0 "
                    + "ELSE (" + MOVE_SIGNED_QTY + " / l.qty_ordered) * " + GROSS_LINE_COMPANY_AMOUNT + " END)";

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
    public List<DeliveredProductFact> deliveredProductFacts(UUID companyId, java.time.LocalDate from,
                                                            java.time.LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT p.id, p.name, "
                        + "COALESCE(SUM(" + MOVE_SIGNED_QTY + "), 0), "
                        + "COALESCE(SUM(" + MOVE_SIGNED_REVENUE + "), 0), "
                        + "COALESCE(SUM(" + MOVE_SIGNED_COST + "), 0) "
                        + "FROM inv_stock_move m "
                        + "JOIN inv_stock_picking pk ON pk.id = m.picking_id "
                        + "JOIN sal_sales_order_line l ON l.id = m.sales_order_line_id "
                        + "JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "JOIN inv_product p ON p.id = COALESCE(l.product_id, m.product_id) "
                        + "WHERE o.company_id = :companyId "
                        + "AND m.state = 'DONE' "
                        + "AND pk.state = 'DONE' "
                        + "AND pk.picking_type IN ('OUTGOING', 'INCOMING') "
                        + "AND m.sales_order_line_id IS NOT NULL "
                        + "AND pk.validated_at IS NOT NULL "
                        + "AND CAST(pk.validated_at AS DATE) >= :fromDate "
                        + "AND CAST(pk.validated_at AS DATE) <= :toDate "
                        + NON_GIFT_LINE + " "
                        + "GROUP BY p.id, p.name "
                        + "HAVING COALESCE(SUM(" + MOVE_SIGNED_QTY + "), 0) > 0");
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        List<Object[]> rows = q.getResultList();
        List<DeliveredProductFact> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new DeliveredProductFact(
                    toUuid(row[0]),
                    row[1] != null ? row[1].toString() : null,
                    toBigDecimal(row[2]),
                    toBigDecimal(row[3]),
                    toBigDecimal(row[4])));
        }
        return result;
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
