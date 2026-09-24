package com.bradox.delin.accounting.service.domain.ports.output.repository;

import com.bradox.delin.domain.core.entity.CustomerPayment;

import java.util.List;
import java.util.UUID;

public interface CustomerPaymentRepository {

    CustomerPayment save(CustomerPayment payment);

    java.util.Optional<CustomerPayment> findById(UUID id);

    List<CustomerPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    List<CustomerPayment> findByCustomerInvoiceId(UUID customerInvoiceId);

    List<CustomerPayment> findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);
}
