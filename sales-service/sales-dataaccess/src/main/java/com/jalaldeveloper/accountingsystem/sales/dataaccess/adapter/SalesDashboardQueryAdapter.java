package com.jalaldeveloper.accountingsystem.sales.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesDashboardResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.SalesDashboardQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SalesDashboardQueryAdapter implements SalesDashboardQueryPort {

    private static final String COMPANY_AMOUNT =
            "(o.amount_total * COALESCE(o.exchange_rate_to_company, 1))";

    private static final String LINE_COMPANY_AMOUNT =
            "((l.qty_ordered * l.unit_price * (1 - COALESCE(l.discount_percent, 0) / 100))"
                    + " * COALESCE(o.exchange_rate_to_company, 1))";

    private static final String CONFIRMED_IN_RANGE =
            "o.state = 'CONFIRMED' AND ("
                    + "(o.order_date IS NOT NULL AND o.order_date >= :fromDate AND o.order_date <= :toDate) "
                    + "OR (o.confirmed_at IS NOT NULL AND CAST(o.confirmed_at AS DATE) >= :fromDate "
                    + "AND CAST(o.confirmed_at AS DATE) <= :toDate))";

    private static final String CONFIRMED_BUCKET_DATE =
            "COALESCE(o.order_date, CAST(o.confirmed_at AS DATE))";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countQuotations(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM sal_sales_order o "
                        + "WHERE o.company_id = :companyId "
                        + "AND o.state IN ('DRAFT', 'QUOTATION_SENT') "
                        + "AND o.order_date IS NOT NULL "
                        + "AND o.order_date >= :fromDate AND o.order_date <= :toDate");
        bindRange(q, companyId, from, to);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long countConfirmedOrders(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM sal_sales_order o "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE);
        bindRange(q, companyId, from, to);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public BigDecimal sumConfirmedRevenue(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(" + COMPANY_AMOUNT + "), 0) FROM sal_sales_order o "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE);
        bindRange(q, companyId, from, to);
        return toBigDecimal(q.getSingleResult());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ConfirmedOrderFact> listConfirmedOrderFacts(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT " + CONFIRMED_BUCKET_DATE + " AS conf_date, " + COMPANY_AMOUNT + " "
                        + "FROM sal_sales_order o "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE);
        bindRange(q, companyId, from, to);
        List<Object[]> rows = q.getResultList();
        List<ConfirmedOrderFact> facts = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            if (date == null) {
                continue;
            }
            facts.add(new ConfirmedOrderFact(date, toBigDecimal(row[1])));
        }
        return facts;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SalesDashboardResponse.SalesRankedOrderRow> topQuotations(
            UUID companyId, LocalDate from, LocalDate to, int limit) {
        Query q = entityManager.createNativeQuery(
                "SELECT o.id, o.name, p.display_name, " + COMPANY_AMOUNT + ", o.order_date "
                        + "FROM sal_sales_order o "
                        + "LEFT JOIN contacts_partner p ON p.id = o.customer_partner_id "
                        + "WHERE o.company_id = :companyId "
                        + "AND o.state IN ('DRAFT', 'QUOTATION_SENT') "
                        + "AND o.order_date IS NOT NULL "
                        + "AND o.order_date >= :fromDate AND o.order_date <= :toDate "
                        + "ORDER BY " + COMPANY_AMOUNT + " DESC");
        bindRange(q, companyId, from, to);
        q.setMaxResults(limit);
        return mapOrderRows(q.getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SalesDashboardResponse.SalesRankedOrderRow> topConfirmedOrders(
            UUID companyId, LocalDate from, LocalDate to, int limit) {
        Query q = entityManager.createNativeQuery(
                "SELECT o.id, o.name, p.display_name, " + COMPANY_AMOUNT + ", o.order_date "
                        + "FROM sal_sales_order o "
                        + "LEFT JOIN contacts_partner p ON p.id = o.customer_partner_id "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE + " "
                        + "ORDER BY " + COMPANY_AMOUNT + " DESC");
        bindRange(q, companyId, from, to);
        q.setMaxResults(limit);
        return mapOrderRows(q.getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SalesDashboardResponse.SalesRankedProductRow> topProducts(
            UUID companyId, LocalDate from, LocalDate to, int limit) {
        Query q = entityManager.createNativeQuery(
                "SELECT p.id, p.name, COUNT(DISTINCT o.id), COALESCE(SUM(" + LINE_COMPANY_AMOUNT + "), 0) "
                        + "FROM sal_sales_order_line l "
                        + "JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "JOIN inv_product p ON p.id = l.product_id "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE + " "
                        + "GROUP BY p.id, p.name "
                        + "ORDER BY COALESCE(SUM(" + LINE_COMPANY_AMOUNT + "), 0) DESC");
        bindRange(q, companyId, from, to);
        q.setMaxResults(limit);
        List<Object[]> rows = q.getResultList();
        List<SalesDashboardResponse.SalesRankedProductRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            SalesDashboardResponse.SalesRankedProductRow item = new SalesDashboardResponse.SalesRankedProductRow();
            item.setProductId(toUuid(row[0]));
            item.setProductName(row[1] != null ? row[1].toString() : null);
            item.setOrderCount(((Number) row[2]).longValue());
            item.setRevenue(toBigDecimal(row[3]));
            result.add(item);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SalesDashboardResponse.SalesCategoryNode> topCategories(
            UUID companyId, LocalDate from, LocalDate to, int limit) {
        Query q = entityManager.createNativeQuery(
                "SELECT c.id, c.name, COUNT(DISTINCT o.id), COALESCE(SUM(" + LINE_COMPANY_AMOUNT + "), 0) "
                        + "FROM sal_sales_order_line l "
                        + "JOIN sal_sales_order o ON o.id = l.sales_order_id "
                        + "JOIN inv_product p ON p.id = l.product_id "
                        + "JOIN inv_product_category c ON c.id = p.category_id "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE + " "
                        + "GROUP BY c.id, c.name "
                        + "ORDER BY COALESCE(SUM(" + LINE_COMPANY_AMOUNT + "), 0) DESC");
        bindRange(q, companyId, from, to);
        q.setMaxResults(limit);
        List<Object[]> rows = q.getResultList();
        List<SalesDashboardResponse.SalesCategoryNode> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            SalesDashboardResponse.SalesCategoryNode item = new SalesDashboardResponse.SalesCategoryNode();
            item.setCategoryId(toUuid(row[0]));
            item.setCategoryName(row[1] != null ? row[1].toString() : null);
            item.setOrderCount(((Number) row[2]).longValue());
            item.setRevenue(toBigDecimal(row[3]));
            result.add(item);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SalesDashboardResponse.SalesNamedAmount> channelSplit(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM pos_order po WHERE po.sales_order_id = o.id) "
                        + "THEN 'POS' ELSE 'SALES' END AS channel_key, "
                        + "COUNT(*), COALESCE(SUM(" + COMPANY_AMOUNT + "), 0) "
                        + "FROM sal_sales_order o "
                        + "WHERE o.company_id = :companyId "
                        + "AND " + CONFIRMED_IN_RANGE + " "
                        + "GROUP BY CASE WHEN EXISTS (SELECT 1 FROM pos_order po WHERE po.sales_order_id = o.id) "
                        + "THEN 'POS' ELSE 'SALES' END");
        bindRange(q, companyId, from, to);
        List<Object[]> rows = q.getResultList();
        List<SalesDashboardResponse.SalesNamedAmount> result = new ArrayList<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "SALES";
            SalesDashboardResponse.SalesNamedAmount item = new SalesDashboardResponse.SalesNamedAmount();
            item.setKey(key);
            item.setLabel("POS".equals(key) ? "Point of Sale" : "Sales orders");
            item.setCount(((Number) row[1]).longValue());
            item.setAmount(toBigDecimal(row[2]));
            result.add(item);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SalesDashboardResponse.SalesNamedAmount> paymentMethodSplit(
            UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT pay.method, COUNT(*), "
                        + "COALESCE(SUM(pay.amount * COALESCE(o.exchange_rate_to_company, 1)), 0) "
                        + "FROM pos_payment pay "
                        + "JOIN pos_order po ON po.id = pay.order_id "
                        + "JOIN sal_sales_order o ON o.id = po.sales_order_id "
                        + "WHERE po.company_id = :companyId "
                        + "AND po.state = 'FINALIZED' "
                        + "AND " + CONFIRMED_IN_RANGE + " "
                        + "GROUP BY pay.method "
                        + "ORDER BY COALESCE(SUM(pay.amount * COALESCE(o.exchange_rate_to_company, 1)), 0) DESC");
        bindRange(q, companyId, from, to);
        List<Object[]> rows = q.getResultList();
        List<SalesDashboardResponse.SalesNamedAmount> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "OTHER";
            SalesDashboardResponse.SalesNamedAmount item = new SalesDashboardResponse.SalesNamedAmount();
            item.setKey(key);
            item.setLabel(humanizePaymentMethod(key));
            item.setCount(((Number) row[1]).longValue());
            item.setAmount(toBigDecimal(row[2]));
            result.add(item);
        }
        return result;
    }

    private static void bindRange(Query q, UUID companyId, LocalDate from, LocalDate to) {
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
    }

    private List<SalesDashboardResponse.SalesRankedOrderRow> mapOrderRows(List<Object[]> rows) {
        List<SalesDashboardResponse.SalesRankedOrderRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            SalesDashboardResponse.SalesRankedOrderRow item = new SalesDashboardResponse.SalesRankedOrderRow();
            item.setId(toUuid(row[0]));
            item.setName(row[1] != null ? row[1].toString() : null);
            item.setCustomerName(row[2] != null ? row[2].toString() : null);
            item.setRevenue(toBigDecimal(row[3]));
            item.setOrderDate(toLocalDate(row[4]));
            result.add(item);
        }
        return result;
    }

    private static String humanizePaymentMethod(String key) {
        return switch (key) {
            case "CASH" -> "Cash";
            case "CARD" -> "Card";
            case "BANK" -> "Bank";
            case "CUSTOMER_ACCOUNT" -> "Customer account";
            default -> key;
        };
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

    private static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return new Date(utilDate.getTime()).toLocalDate();
        }
        return LocalDate.parse(value.toString());
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
