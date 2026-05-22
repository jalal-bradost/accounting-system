package com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurVendorPaymentJpaRepository extends JpaRepository<PurVendorPaymentEntity, UUID> {

    List<PurVendorPaymentEntity> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    List<PurVendorPaymentEntity> findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(UUID companyId,
                                                                                                    UUID vendorPartnerId);
}
