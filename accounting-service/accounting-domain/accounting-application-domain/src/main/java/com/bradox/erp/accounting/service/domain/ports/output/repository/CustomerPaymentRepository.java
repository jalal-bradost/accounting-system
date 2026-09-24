package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.entity.CustomerPayment;

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
