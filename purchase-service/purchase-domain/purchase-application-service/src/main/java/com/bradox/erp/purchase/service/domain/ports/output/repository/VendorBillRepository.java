package com.bradox.erp.purchase.service.domain.ports.output.repository;

import com.bradox.erp.purchase.domain.core.entity.VendorBill;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorBillRepository {

    VendorBill save(VendorBill bill);

    Optional<VendorBill> findById(UUID id);

    List<VendorBill> findByPurchaseOrderId(UUID purchaseOrderId);

    List<VendorBill> findByPurchaseOrderIdIn(Collection<UUID> purchaseOrderIds);

    Page<VendorBill> searchByCompanyId(UUID companyId, Pageable pageable);

    List<VendorBill> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before);

    List<VendorBill> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to);

    List<VendorBill> findByCompanyIdOrderByBillDateDescCreatedAtDesc(UUID companyId);

    List<VendorBill> findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(UUID companyId,
                                                                                     UUID vendorPartnerId);

    List<VendorBill> findByReversedBillId(UUID reversedBillId);

    List<VendorBill> findByReversedBillIdIn(java.util.Collection<UUID> reversedBillIds);

    List<VendorBill> findByIdIn(java.util.Collection<UUID> ids);

    /** Acquires a pessimistic write lock on the bill row (held until the transaction ends). */
    void lockById(UUID id);

    List<VendorBill> findOpeningBalanceByCompanyId(UUID companyId);
}
