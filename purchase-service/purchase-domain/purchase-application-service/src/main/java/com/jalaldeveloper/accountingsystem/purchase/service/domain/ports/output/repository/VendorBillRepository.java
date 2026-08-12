package com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBill;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorBillRepository {

    VendorBill save(VendorBill bill);

    Optional<VendorBill> findById(UUID id);

    List<VendorBill> findByPurchaseOrderId(UUID purchaseOrderId);

    List<VendorBill> findByCompanyIdOrderByBillDateDescCreatedAtDesc(UUID companyId);

    List<VendorBill> findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(UUID companyId,
                                                                                     UUID vendorPartnerId);

    List<VendorBill> findByReversedBillId(UUID reversedBillId);
}
