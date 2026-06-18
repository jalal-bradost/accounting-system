package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerPayment;

import java.util.List;
import java.util.UUID;

public interface CustomerPaymentRepository {

    CustomerPayment save(CustomerPayment payment);

    List<CustomerPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    List<CustomerPayment> findByCustomerInvoiceId(UUID customerInvoiceId);

    List<CustomerPayment> findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);
}
