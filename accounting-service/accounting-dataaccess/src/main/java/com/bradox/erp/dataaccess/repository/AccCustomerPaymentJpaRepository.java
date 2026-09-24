package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.AccCustomerPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccCustomerPaymentJpaRepository extends JpaRepository<AccCustomerPaymentEntity, UUID> {

    List<AccCustomerPaymentEntity> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    List<AccCustomerPaymentEntity> findByCustomerInvoiceId(UUID customerInvoiceId);

    List<AccCustomerPaymentEntity> findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);
}
