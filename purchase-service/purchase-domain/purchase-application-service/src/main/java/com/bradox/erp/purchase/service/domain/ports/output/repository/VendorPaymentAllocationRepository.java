package com.bradox.erp.purchase.service.domain.ports.output.repository;

import com.bradox.erp.purchase.domain.core.entity.VendorPaymentAllocation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorPaymentAllocationRepository {

    VendorPaymentAllocation save(VendorPaymentAllocation allocation);

    Optional<VendorPaymentAllocation> findById(UUID id);

    List<VendorPaymentAllocation> findByPaymentIdIn(Collection<UUID> paymentIds);

    List<VendorPaymentAllocation> findByBillIdIn(Collection<UUID> billIds);

    List<VendorPaymentAllocation> findByCompanyId(UUID companyId);
}
