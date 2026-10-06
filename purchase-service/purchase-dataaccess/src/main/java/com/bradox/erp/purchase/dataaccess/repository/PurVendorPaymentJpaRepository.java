package com.bradox.erp.purchase.dataaccess.repository;

import com.bradox.erp.purchase.dataaccess.entity.PurVendorPaymentEntity;
import com.bradox.erp.purchase.domain.core.VendorPaymentState;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurVendorPaymentJpaRepository extends JpaRepository<PurVendorPaymentEntity, UUID> {

    List<PurVendorPaymentEntity> findByIdIn(Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurVendorPaymentEntity p where p.id = :id")
    Optional<PurVendorPaymentEntity> findByIdForUpdate(@Param("id") UUID id);

    List<PurVendorPaymentEntity> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    @Query(
            value = "select p.id from PurVendorPaymentEntity p where p.companyId = :companyId order by p.paymentDate desc, p.createdAt desc",
            countQuery = "select count(p) from PurVendorPaymentEntity p where p.companyId = :companyId")
    Page<UUID> findIdsByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    List<PurVendorPaymentEntity> findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(UUID companyId,
                                                                                                    UUID vendorPartnerId);

    @Query("""
            select p from PurVendorPaymentEntity p
            where p.companyId = :companyId and p.vendorPartnerId = :partnerId
              and p.state = :state and p.paymentDate < :before
            order by p.paymentDate asc, p.createdAt asc
            """)
    List<PurVendorPaymentEntity> findPostedByPartnerBefore(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") VendorPaymentState state,
            @Param("before") LocalDateTime before);

    @Query("""
            select p from PurVendorPaymentEntity p
            where p.companyId = :companyId and p.vendorPartnerId = :partnerId
              and p.state = :state and p.paymentDate >= :from and p.paymentDate < :toExclusive
            order by p.paymentDate asc, p.createdAt asc
            """)
    List<PurVendorPaymentEntity> findPostedByPartnerBetween(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") VendorPaymentState state,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    List<PurVendorPaymentEntity> findByCompanyIdAndOpeningBalanceTrueOrderByPaymentDateAscCreatedAtAsc(UUID companyId);
}
