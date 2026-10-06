package com.bradox.erp.purchase.dataaccess.repository;

import com.bradox.erp.purchase.dataaccess.entity.PurVendorBillEntity;
import com.bradox.erp.purchase.domain.core.VendorBillState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PurVendorBillJpaRepository extends JpaRepository<PurVendorBillEntity, UUID> {

    List<PurVendorBillEntity> findByPurchaseOrderId(UUID purchaseOrderId);

    List<PurVendorBillEntity> findByPurchaseOrderIdIn(Collection<UUID> purchaseOrderIds);

    List<PurVendorBillEntity> findByCompanyIdOrderByBillDateDescCreatedAtDesc(UUID companyId);

    @Query(
            value = "select b.id from PurVendorBillEntity b where b.companyId = :companyId order by b.billDate desc, b.createdAt desc",
            countQuery = "select count(b) from PurVendorBillEntity b where b.companyId = :companyId")
    Page<UUID> findIdsByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    List<PurVendorBillEntity> findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(UUID companyId,
                                                                                             UUID vendorPartnerId);

    @Query("""
            select b from PurVendorBillEntity b
            where b.companyId = :companyId and b.vendorPartnerId = :partnerId
              and b.state = :state and b.billDate < :before
            order by b.billDate asc, b.createdAt asc
            """)
    List<PurVendorBillEntity> findPostedByPartnerBefore(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") VendorBillState state,
            @Param("before") LocalDate before);

    @Query("""
            select b from PurVendorBillEntity b
            where b.companyId = :companyId and b.vendorPartnerId = :partnerId
              and b.state = :state and b.billDate >= :from and b.billDate <= :to
            order by b.billDate asc, b.createdAt asc
            """)
    List<PurVendorBillEntity> findPostedByPartnerBetween(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") VendorBillState state,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    List<PurVendorBillEntity> findByReversedBillIdOrderByBillDateDescCreatedAtDesc(UUID reversedBillId);

    List<PurVendorBillEntity> findByReversedBillIdIn(java.util.Collection<UUID> reversedBillIds);

    List<PurVendorBillEntity> findByIdIn(java.util.Collection<UUID> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from PurVendorBillEntity b where b.id = :id")
    java.util.Optional<PurVendorBillEntity> lockById(@org.springframework.data.repository.query.Param("id") UUID id);

    List<PurVendorBillEntity> findByCompanyIdAndOpeningBalanceTrueOrderByBillDateAscCreatedAtAsc(UUID companyId);
}
