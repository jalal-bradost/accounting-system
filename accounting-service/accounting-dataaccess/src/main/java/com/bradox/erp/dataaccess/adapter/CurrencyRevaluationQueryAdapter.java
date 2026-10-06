package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.CurrencyRevaluationQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CurrencyRevaluationQueryAdapter implements CurrencyRevaluationQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<OpenForeignBalance> findOpenForeignBalances(UUID companyId, LocalDate asOf, String baseCurrency) {
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT i.account_id, a.code, a.name, COALESCE(i.partner_id, e.partner_id), "
                        + "MAX(COALESCE(i.partner_name, e.partner_name)), i.currency_code, "
                        + "COALESCE(SUM(i.amount_currency), 0), COALESCE(SUM(i.debit - i.credit), 0) "
                        + "FROM journal_items i "
                        + "JOIN journal_entries e ON e.id = i.journal_entry_id "
                        + "JOIN accounts a ON a.id = i.account_id "
                        + "WHERE e.company_id = :companyId AND e.status = 'POSTED' "
                        + "AND e.entry_date < :endExclusive "
                        + "AND a.type IN ('RECEIVABLE', 'PAYABLE') "
                        + "AND i.currency_code IS NOT NULL AND i.currency_code <> :base "
                        + "GROUP BY i.account_id, a.code, a.name, COALESCE(i.partner_id, e.partner_id), i.currency_code "
                        + "ORDER BY a.code, 5, i.currency_code")
                .setParameter("companyId", companyId.toString())
                .setParameter("endExclusive", asOf.plusDays(1).atStartOfDay())
                .setParameter("base", baseCurrency)
                .getResultList();
        List<OpenForeignBalance> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new OpenForeignBalance(
                    toUuid(r[0]), (String) r[1], (String) r[2], toUuid(r[3]), (String) r[4], (String) r[5],
                    (BigDecimal) r[6], (BigDecimal) r[7]));
        }
        return out;
    }

    private static UUID toUuid(Object o) {
        if (o == null) return null;
        if (o instanceof UUID u) return u;
        if (o instanceof byte[] bytes) {
            if (bytes.length == 16) {
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
                return new UUID(bb.getLong(), bb.getLong());
            }
            return UUID.fromString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }
        return UUID.fromString(o.toString());
    }
}
