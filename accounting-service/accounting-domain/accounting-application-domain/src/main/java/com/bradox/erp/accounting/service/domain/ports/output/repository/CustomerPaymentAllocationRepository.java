package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.entity.CustomerPaymentAllocation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPaymentAllocationRepository {

    CustomerPaymentAllocation save(CustomerPaymentAllocation allocation);

    Optional<CustomerPaymentAllocation> findById(UUID id);

    List<CustomerPaymentAllocation> findByPaymentIdIn(Collection<UUID> paymentIds);

    List<CustomerPaymentAllocation> findByInvoiceIdIn(Collection<UUID> invoiceIds);

    List<CustomerPaymentAllocation> findByCompanyId(UUID companyId);
}
