package com.bradox.erp.purchase.dataaccess.repository;

import com.bradox.erp.purchase.dataaccess.entity.PurVendorPaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PurVendorPaymentAllocationJpaRepository extends JpaRepository<PurVendorPaymentAllocationEntity, UUID> {

    List<PurVendorPaymentAllocationEntity> findByPaymentIdInOrderByCreatedAtAsc(Collection<UUID> paymentIds);

    List<PurVendorPaymentAllocationEntity> findByVendorBillIdInOrderByCreatedAtAsc(Collection<UUID> billIds);

    List<PurVendorPaymentAllocationEntity> findByCompanyId(UUID companyId);
}
