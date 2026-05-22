package com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorBillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurVendorBillJpaRepository extends JpaRepository<PurVendorBillEntity, UUID> {

    List<PurVendorBillEntity> findByPurchaseOrderId(UUID purchaseOrderId);

    List<PurVendorBillEntity> findByCompanyIdOrderByBillDateDescCreatedAtDesc(UUID companyId);

    List<PurVendorBillEntity> findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(UUID companyId,
                                                                                             UUID vendorPartnerId);
}
