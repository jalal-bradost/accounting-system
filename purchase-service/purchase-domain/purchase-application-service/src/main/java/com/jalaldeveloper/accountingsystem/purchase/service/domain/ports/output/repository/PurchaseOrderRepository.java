package com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderState;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository {

    PurchaseOrder save(PurchaseOrder order);

    Optional<PurchaseOrder> findById(UUID id);

    /** Loads the PO from the database with a row lock so concurrent receive/bill updates cannot use a stale version. */
    Optional<PurchaseOrder> findByIdForUpdate(UUID id);

    Optional<PurchaseOrder> findByCompanyIdAndName(UUID companyId, String name);

    Page<PurchaseOrder> search(UUID companyId,
                               PurchaseOrderState state,
                               UUID vendorPartnerId,
                               String q,
                               Pageable pageable);

    void flush();
}
