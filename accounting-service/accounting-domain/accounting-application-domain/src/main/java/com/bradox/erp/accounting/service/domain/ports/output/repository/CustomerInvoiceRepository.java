package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.core.entity.CustomerInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerInvoiceRepository {

    CustomerInvoice save(CustomerInvoice invoice);

    Optional<CustomerInvoice> findById(UUID id);

    /** Removes an invoice with its lines and tax snapshots (drafts only; callers enforce it). */
    void deleteById(UUID id);

    Optional<CustomerInvoice> findByIdWithLines(UUID id);

    List<CustomerInvoice> findByCompanyWithLines(UUID companyId);

    /** Paginated header search; returned invoices include lines. */
    Page<CustomerInvoice> search(UUID companyId, CustomerInvoiceState state, UUID salesOrderId, String q, Pageable pageable);

    boolean existsBySalesOrderIdAndState(UUID salesOrderId, CustomerInvoiceState state);

    List<CustomerInvoice> findBySalesOrderIdWithLines(UUID salesOrderId);

    List<CustomerInvoice> findBySalesOrderIdInWithLines(Collection<UUID> salesOrderIds);

    List<CustomerInvoice> findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);

    List<CustomerInvoice> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before);

    List<CustomerInvoice> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to);

    List<CustomerInvoice> findByReversedInvoiceIdWithLines(UUID reversedInvoiceId);

    List<CustomerInvoice> findByReversedInvoiceIdIn(java.util.Collection<UUID> reversedInvoiceIds);

    List<CustomerInvoice> findByIdIn(java.util.Collection<UUID> ids);

    /** Acquires a pessimistic write lock on the invoice row (held until the transaction ends). */
    void lockById(UUID id);

    List<CustomerInvoice> findOpeningBalanceByCompanyId(UUID companyId);
}
