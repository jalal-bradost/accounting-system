package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.dashboard.AccountingDashboardResponse;
import com.bradox.erp.accounting.service.domain.ports.output.AccountingDashboardQueryPort;
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
public class AccountingDashboardQueryAdapter implements AccountingDashboardQueryPort {

    private static final String INVOICE_SIGN =
            "(CASE WHEN i.move_type = 'CREDIT_NOTE' THEN -1 ELSE 1 END)";

    private static final String INVOICE_LINES =
            "COALESCE((SELECT SUM(l.qty * l.unit_price * (1 - COALESCE(l.discount_percent, 0) / 100)) "
                    + "FROM acc_customer_invoice_line l WHERE l.customer_invoice_id = i.id), 0)";

    private static final String INVOICE_TAXES =
            "COALESCE((SELECT SUM(t.tax_amount) FROM acc_customer_invoice_line_tax t "
                    + "JOIN acc_customer_invoice_line l ON l.id = t.line_id "
                    + "WHERE l.customer_invoice_id = i.id), 0)";

    private static final String INVOICE_COMPANY_AMOUNT =
            "(" + INVOICE_SIGN + " * (" + INVOICE_LINES + " + " + INVOICE_TAXES + ")"
                    + " * COALESCE(i.exchange_rate_to_company, 1))";

    private static final String INVOICE_IN_RANGE =
            "i.state = 'POSTED' "
                    + "AND i.invoice_date IS NOT NULL "
                    + "AND i.invoice_date >= :fromDate AND i.invoice_date <= :toDate";

    private static final String BILL_SIGN =
            "(CASE WHEN b.move_type = 'CREDIT_NOTE' THEN -1 ELSE 1 END)";

    private static final String BILL_LINES =
            "COALESCE((SELECT SUM(l.qty * l.unit_price) FROM pur_vendor_bill_line l "
                    + "WHERE l.vendor_bill_id = b.id), 0)";

    private static final String BILL_TAXES =
            "COALESCE((SELECT SUM(t.tax_amount) FROM pur_vendor_bill_line_tax t "
                    + "JOIN pur_vendor_bill_line l ON l.id = t.line_id "
                    + "WHERE l.vendor_bill_id = b.id), 0)";

    private static final String BILL_COMPANY_AMOUNT =
            "(" + BILL_SIGN + " * (" + BILL_LINES + " + " + BILL_TAXES + ")"
                    + " * COALESCE(b.exchange_rate_to_company, 1))";

    private static final String BILL_IN_RANGE =
            "b.state = 'POSTED' "
                    + "AND b.bill_date IS NOT NULL "
                    + "AND b.bill_date >= :fromDate AND b.bill_date <= :toDate";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countPostedInvoices(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM acc_customer_invoice i "
                        + "WHERE i.company_id = :companyId "
                        + "AND " + INVOICE_IN_RANGE + " "
                        + "AND i.move_type <> 'CREDIT_NOTE'");
        bindRange(q, companyId, from, to);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long countPostedBills(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM pur_vendor_bill b "
                        + "WHERE b.company_id = :companyId "
                        + "AND " + BILL_IN_RANGE + " "
                        + "AND b.move_type <> 'CREDIT_NOTE'");
        bindRange(q, companyId, from, to);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public BigDecimal sumPostedIncome(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(" + INVOICE_COMPANY_AMOUNT + "), 0) FROM acc_customer_invoice i "
                        + "WHERE i.company_id = :companyId "
                        + "AND " + INVOICE_IN_RANGE);
        bindRange(q, companyId, from, to);
        return toBigDecimal(q.getSingleResult());
    }

    @Override
    public BigDecimal sumPostedSpend(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(" + BILL_COMPANY_AMOUNT + "), 0) FROM pur_vendor_bill b "
                        + "WHERE b.company_id = :companyId "
                        + "AND " + BILL_IN_RANGE);
        bindRange(q, companyId, from, to);
        return toBigDecimal(q.getSingleResult());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DocumentFact> listInvoiceFacts(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT i.invoice_date, " + INVOICE_COMPANY_AMOUNT + " "
                        + "FROM acc_customer_invoice i "
                        + "WHERE i.company_id = :companyId "
                        + "AND " + INVOICE_IN_RANGE);
        bindRange(q, companyId, from, to);
        return mapFacts(q.getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DocumentFact> listBillFacts(UUID companyId, LocalDate from, LocalDate to) {
        Query q = entityManager.createNativeQuery(
                "SELECT b.bill_date, " + BILL_COMPANY_AMOUNT + " "
                        + "FROM pur_vendor_bill b "
                        + "WHERE b.company_id = :companyId "
                        + "AND " + BILL_IN_RANGE);
        bindRange(q, companyId, from, to);
        return mapFacts(q.getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AccountingDashboardResponse.RankedDocumentRow> topInvoices(
            UUID companyId, LocalDate from, LocalDate to, int limit) {
        Query q = entityManager.createNativeQuery(
                "SELECT i.id, i.reference, p.display_name, " + INVOICE_COMPANY_AMOUNT + ", i.invoice_date "
                        + "FROM acc_customer_invoice i "
                        + "LEFT JOIN contacts_partner p ON p.id = i.customer_partner_id "
                        + "WHERE i.company_id = :companyId "
                        + "AND " + INVOICE_IN_RANGE + " "
                        + "AND i.move_type <> 'CREDIT_NOTE' "
                        + "ORDER BY " + INVOICE_COMPANY_AMOUNT + " DESC");
        bindRange(q, companyId, from, to);
        q.setMaxResults(limit);
        return mapDocuments(q.getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AccountingDashboardResponse.RankedDocumentRow> topBills(
            UUID companyId, LocalDate from, LocalDate to, int limit) {
        Query q = entityManager.createNativeQuery(
                "SELECT b.id, b.reference, p.display_name, " + BILL_COMPANY_AMOUNT + ", b.bill_date "
                        + "FROM pur_vendor_bill b "
                        + "LEFT JOIN contacts_partner p ON p.id = b.vendor_partner_id "
                        + "WHERE b.company_id = :companyId "
                        + "AND " + BILL_IN_RANGE + " "
                        + "AND b.move_type <> 'CREDIT_NOTE' "
                        + "ORDER BY " + BILL_COMPANY_AMOUNT + " DESC");
        bindRange(q, companyId, from, to);
        q.setMaxResults(limit);
        return mapDocuments(q.getResultList());
    }

    private static void bindRange(Query q, UUID companyId, LocalDate from, LocalDate to) {
        q.setParameter("companyId", companyId);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
    }

    private List<DocumentFact> mapFacts(List<Object[]> rows) {
        List<DocumentFact> facts = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            if (date == null) {
                continue;
            }
            facts.add(new DocumentFact(date, toBigDecimal(row[1])));
        }
        return facts;
    }

    private List<AccountingDashboardResponse.RankedDocumentRow> mapDocuments(List<Object[]> rows) {
        List<AccountingDashboardResponse.RankedDocumentRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            AccountingDashboardResponse.RankedDocumentRow item = new AccountingDashboardResponse.RankedDocumentRow();
            item.setId(toUuid(row[0]));
            item.setName(row[1] != null ? row[1].toString() : null);
            String partner = row[2] != null ? row[2].toString() : null;
            item.setCustomerName(partner);
            item.setPartnerName(partner);
            item.setRevenue(toBigDecimal(row[3]));
            item.setOrderDate(toLocalDate(row[4]));
            result.add(item);
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
