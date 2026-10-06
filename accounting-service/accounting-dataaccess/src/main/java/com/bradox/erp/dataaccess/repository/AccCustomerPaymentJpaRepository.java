package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.AccCustomerPaymentEntity;
import com.bradox.erp.domain.core.ValueObject.CustomerPaymentState;
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

public interface AccCustomerPaymentJpaRepository extends JpaRepository<AccCustomerPaymentEntity, UUID> {

    List<AccCustomerPaymentEntity> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    @Query(
            value = "select p.id from AccCustomerPaymentEntity p where p.companyId = :companyId order by p.paymentDate desc, p.createdAt desc",
            countQuery = "select count(p) from AccCustomerPaymentEntity p where p.companyId = :companyId")
    Page<UUID> findIdsByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    List<AccCustomerPaymentEntity> findByIdIn(Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AccCustomerPaymentEntity p where p.id = :id")
    Optional<AccCustomerPaymentEntity> findByIdForUpdate(@Param("id") UUID id);

    List<AccCustomerPaymentEntity> findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);

    @Query("""
            select p from AccCustomerPaymentEntity p
            where p.companyId = :companyId and p.customerPartnerId = :partnerId
              and p.state = :state and p.paymentDate < :before
            order by p.paymentDate asc, p.createdAt asc
            """)
    List<AccCustomerPaymentEntity> findPostedByPartnerBefore(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") CustomerPaymentState state,
            @Param("before") LocalDateTime before);

    @Query("""
            select p from AccCustomerPaymentEntity p
            where p.companyId = :companyId and p.customerPartnerId = :partnerId
              and p.state = :state and p.paymentDate >= :from and p.paymentDate < :toExclusive
            order by p.paymentDate asc, p.createdAt asc
            """)
    List<AccCustomerPaymentEntity> findPostedByPartnerBetween(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") CustomerPaymentState state,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    List<AccCustomerPaymentEntity> findByCompanyIdAndOpeningBalanceTrueOrderByPaymentDateAscCreatedAtAsc(UUID companyId);
}
