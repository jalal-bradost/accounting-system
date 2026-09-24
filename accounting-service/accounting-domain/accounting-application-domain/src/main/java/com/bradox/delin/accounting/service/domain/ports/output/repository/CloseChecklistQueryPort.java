package com.bradox.delin.accounting.service.domain.ports.output.repository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Counts open documents that block month close for a date range.
 */
public interface CloseChecklistQueryPort {

    int countDraftJournalEntries(UUID companyId, LocalDate fromInclusive, LocalDate toInclusive);

    int countDraftCustomerInvoices(UUID companyId, LocalDate fromInclusive, LocalDate toInclusive);

    int countDraftVendorBills(UUID companyId, LocalDate fromInclusive, LocalDate toInclusive);
}
