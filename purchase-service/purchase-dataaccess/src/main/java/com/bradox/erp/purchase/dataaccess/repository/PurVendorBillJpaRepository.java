package com.bradox.erp.purchase.dataaccess.repository;

import com.bradox.erp.purchase.dataaccess.entity.PurVendorBillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurVendorBillJpaRepository extends JpaRepository<PurVendorBillEntity, UUID> {

    List<PurVendorBillEntity> findByPurchaseOrderId(UUID purchaseOrderId);

    List<PurVendorBillEntity> findByCompanyIdOrderByBillDateDescCreatedAtDesc(UUID companyId);

    List<PurVendorBillEntity> findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(UUID companyId,
                                                                                             UUID vendorPartnerId);

    List<PurVendorBillEntity> findByReversedBillIdOrderByBillDateDescCreatedAtDesc(UUID reversedBillId);
}
