package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.CloseChecklistQueryPort;
import com.bradox.erp.dataaccess.repository.AccCustomerInvoiceJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalEntryJpaRepository;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.core.ValueObject.JournalEntryStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class CloseChecklistQueryAdapter implements CloseChecklistQueryPort {

    private final JournalEntryJpaRepository journalEntryJpaRepository;
    private final AccCustomerInvoiceJpaRepository customerInvoiceJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CloseChecklistQueryAdapter(JournalEntryJpaRepository journalEntryJpaRepository,
                                      AccCustomerInvoiceJpaRepository customerInvoiceJpaRepository) {
        this.journalEntryJpaRepository = journalEntryJpaRepository;
        this.customerInvoiceJpaRepository = customerInvoiceJpaRepository;
    }

    @Override
    public int countDraftJournalEntries(UUID companyId, LocalDate fromInclusive, LocalDate toInclusive) {
        LocalDateTime from = fromInclusive.atStartOfDay();
        LocalDateTime toExclusive = toInclusive.plusDays(1).atStartOfDay();
        return (int) journalEntryJpaRepository.findByCompanyId(companyId).stream()
                .filter(e -> e.getStatus() == JournalEntryStatus.DRAFT)
                .filter(e -> {
                    LocalDateTime d = e.getEntryDate();
                    return d != null && !d.isBefore(from) && d.isBefore(toExclusive);
                })
                .count();
    }

    @Override
    public int countDraftCustomerInvoices(UUID companyId, LocalDate fromInclusive, LocalDate toInclusive) {
        return (int) customerInvoiceJpaRepository.findByCompanyWithLines(companyId).stream()
                .filter(e -> e.getState() == CustomerInvoiceState.DRAFT)
                .filter(e -> {
                    LocalDate d = e.getInvoiceDate();
                    return d != null && !d.isBefore(fromInclusive) && !d.isAfter(toInclusive);
                })
                .count();
    }

    @Override
    public int countDraftVendorBills(UUID companyId, LocalDate fromInclusive, LocalDate toInclusive) {
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM pur_vendor_bill "
                                + "WHERE company_id = :companyId AND state = 'DRAFT' "
                                + "AND bill_date >= :fromDate AND bill_date <= :toDate")
                .setParameter("companyId", companyId)
                .setParameter("fromDate", fromInclusive)
                .setParameter("toDate", toInclusive)
                .getSingleResult();
        return count != null ? count.intValue() : 0;
    }
}
