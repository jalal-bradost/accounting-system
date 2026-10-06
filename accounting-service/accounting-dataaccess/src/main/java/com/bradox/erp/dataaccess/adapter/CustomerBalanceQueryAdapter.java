package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.CustomerBalanceQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class CustomerBalanceQueryAdapter implements CustomerBalanceQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<CustomerBalance> findBalances(UUID companyId, Collection<UUID> partnerIds) {
        boolean filtered = partnerIds != null && !partnerIds.isEmpty();
        String sql = "SELECT COALESCE(i.partner_id, e.partner_id) AS pid, SUM(i.debit - i.credit) "
                + "FROM journal_items i "
                + "JOIN journal_entries e ON e.id = i.journal_entry_id "
                + "JOIN accounts a ON a.id = i.account_id "
                + "WHERE e.company_id = :companyId AND e.status = 'POSTED' AND a.type = 'RECEIVABLE' "
                + "AND COALESCE(i.partner_id, e.partner_id) IS NOT NULL "
                + (filtered ? "AND COALESCE(i.partner_id, e.partner_id) IN (:partnerIds) " : "")
                + "GROUP BY COALESCE(i.partner_id, e.partner_id)";
        var query = entityManager.createNativeQuery(sql).setParameter("companyId", companyId.toString());
        if (filtered) {
            query.setParameter("partnerIds", partnerIds.stream().map(UUID::toString).toList());
        }
        List<CustomerBalance> out = new ArrayList<>();
        for (Object[] r : (List<Object[]>) query.getResultList()) {
            out.add(new CustomerBalance(toUuid(r[0]), r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO));
        }
        return out;
    }

    private static UUID toUuid(Object o) {
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
