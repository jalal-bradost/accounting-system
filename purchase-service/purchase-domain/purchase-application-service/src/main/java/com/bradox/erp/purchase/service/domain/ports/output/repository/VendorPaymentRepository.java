package com.bradox.erp.purchase.service.domain.ports.output.repository;

import com.bradox.erp.purchase.domain.core.entity.VendorPayment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorPaymentRepository {

    VendorPayment save(VendorPayment payment);

    Optional<VendorPayment> findById(UUID id);

    /** Loads the payment under a pessimistic write lock (held until the transaction ends). */
    Optional<VendorPayment> findByIdForUpdate(UUID id);

    List<VendorPayment> findByIdIn(Collection<UUID> ids);

    List<VendorPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    Page<VendorPayment> searchByCompanyId(UUID companyId, Pageable pageable);

    List<VendorPayment> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before);

    List<VendorPayment> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to);

    List<VendorPayment> findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(UUID companyId,
                                                                                          UUID vendorPartnerId);

    List<VendorPayment> findOpeningBalanceByCompanyId(UUID companyId);
}
