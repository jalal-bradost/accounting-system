package com.bradox.erp.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Output port for opening payable documents/payments without depending on purchase at the Maven level.
 */
public interface OpeningPayablePort {

    UUID createOpeningVendorBill(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                                 LocalDate date, LocalDate dueDate, String reference,
                                 UUID openingJournalId, UUID openingEquityAccountId);

    UUID postOpeningPayment(UUID companyId, UUID partnerId, BigDecimal amount, String currency,
                            LocalDate date, String reference,
                            UUID openingJournalId, UUID openingEquityAccountId);

    List<OpeningVendorBillRef> findOpeningBills(UUID companyId);

    List<OpeningVendorPaymentRef> findOpeningPayments(UUID companyId);

    boolean hasActiveAllocations(Collection<UUID> billIds, Collection<UUID> paymentIds);

    boolean hasCreditNotes(Collection<UUID> billIds);

    void cancelOpeningBill(UUID billId);

    void reverseOpeningPayment(UUID paymentId, String reason);

    record OpeningVendorBillRef(UUID id, String reference, UUID journalEntryId) {}

    record OpeningVendorPaymentRef(UUID id, String reference, UUID journalEntryId) {}
}
