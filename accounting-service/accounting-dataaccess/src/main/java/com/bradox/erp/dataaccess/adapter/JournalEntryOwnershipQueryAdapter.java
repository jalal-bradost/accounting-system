package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.JournalEntryOwnershipPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JournalEntryOwnershipQueryAdapter implements JournalEntryOwnershipPort {

    /** Every document column that points at a journal entry it posted. */
    private static final String OWNER_SQL =
            "SELECT CONCAT('customer invoice ', COALESCE(reference, '')) FROM acc_customer_invoice "
                    + "WHERE journal_entry_id = :id "
                    + "UNION ALL SELECT 'customer payment' FROM acc_customer_payment "
                    + "WHERE journal_entry_id = :id OR reversal_journal_entry_id = :id "
                    + "UNION ALL SELECT 'customer payment allocation' FROM acc_customer_payment_allocation "
                    + "WHERE fx_journal_entry_id = :id OR fx_reversal_journal_entry_id = :id "
                    + "UNION ALL SELECT CONCAT('vendor bill ', COALESCE(reference, '')) FROM pur_vendor_bill "
                    + "WHERE journal_entry_id = :id "
                    + "UNION ALL SELECT 'vendor payment' FROM pur_vendor_payment "
                    + "WHERE journal_entry_id = :id OR reversal_journal_entry_id = :id "
                    + "UNION ALL SELECT 'vendor payment allocation' FROM pur_vendor_payment_allocation "
                    + "WHERE fx_journal_entry_id = :id OR fx_reversal_journal_entry_id = :id "
                    + "UNION ALL SELECT 'expense' FROM exp_expense "
                    + "WHERE journal_entry_id = :id OR payment_journal_entry_id = :id "
                    + "UNION ALL SELECT 'stock valuation' FROM inv_stock_valuation_layer "
                    + "WHERE journal_entry_id = :id "
                    + "UNION ALL SELECT 'payslip' FROM pay_payslip WHERE journal_entry_id = :id "
                    + "UNION ALL SELECT 'pay run' FROM pay_run "
                    + "WHERE journal_entry_id = :id OR payment_journal_entry_id = :id "
                    + "UNION ALL SELECT 'sales commission' FROM sal_commission_snapshot "
                    + "WHERE journal_entry_id = :id "
                    + "UNION ALL SELECT 'salesperson cash entry' FROM sal_salesperson_cash_entry "
                    + "WHERE journal_entry_id = :id";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> findOwner(UUID journalEntryId) {
        if (journalEntryId == null) {
            return Optional.empty();
        }
        Query q = entityManager.createNativeQuery(OWNER_SQL);
        q.setParameter("id", journalEntryId);
        q.setMaxResults(1);
        List<Object> rows = q.getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0).toString().trim());
    }
}
