package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.AccCustomerPaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AccCustomerPaymentAllocationJpaRepository extends JpaRepository<AccCustomerPaymentAllocationEntity, UUID> {

    List<AccCustomerPaymentAllocationEntity> findByPaymentIdInOrderByCreatedAtAsc(Collection<UUID> paymentIds);

    List<AccCustomerPaymentAllocationEntity> findByCustomerInvoiceIdInOrderByCreatedAtAsc(Collection<UUID> invoiceIds);

    List<AccCustomerPaymentAllocationEntity> findByCompanyId(UUID companyId);
}
