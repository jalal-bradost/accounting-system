package com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorPayment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorPaymentRepository {

    VendorPayment save(VendorPayment payment);

    Optional<VendorPayment> findById(UUID id);

    List<VendorPayment> findByVendorBillId(UUID vendorBillId);

    List<VendorPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    List<VendorPayment> findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(UUID companyId,
                                                                                          UUID vendorPartnerId);
}
