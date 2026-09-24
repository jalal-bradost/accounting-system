package com.bradox.delin.accounting.service.domain.ports.output.repository;

import com.bradox.delin.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.delin.domain.core.entity.CustomerInvoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerInvoiceRepository {

    CustomerInvoice save(CustomerInvoice invoice);

    Optional<CustomerInvoice> findById(UUID id);

    Optional<CustomerInvoice> findByIdWithLines(UUID id);

    List<CustomerInvoice> findByCompanyWithLines(UUID companyId);

    boolean existsBySalesOrderIdAndState(UUID salesOrderId, CustomerInvoiceState state);

    List<CustomerInvoice> findBySalesOrderIdWithLines(UUID salesOrderId);

    List<CustomerInvoice> findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);

    List<CustomerInvoice> findByReversedInvoiceIdWithLines(UUID reversedInvoiceId);
}
