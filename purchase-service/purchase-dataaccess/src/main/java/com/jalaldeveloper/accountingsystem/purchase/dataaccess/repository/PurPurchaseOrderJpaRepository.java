package com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurPurchaseOrderEntity;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PurPurchaseOrderJpaRepository extends JpaRepository<PurPurchaseOrderEntity, UUID> {

    Optional<PurPurchaseOrderEntity> findByCompanyIdAndName(UUID companyId, String name);

    // Pass empty string (never null) for :q — Postgres + Hibernate bind null strings as bytea,
    // which breaks lower(concat('%', :q, '%')).
    @Query("""
            select o from PurPurchaseOrderEntity o
            where o.companyId = :companyId
            and (:state is null or o.state = :state)
            and (:vendorId is null or o.vendorPartnerId = :vendorId)
            and (:q = '' or lower(o.name) like concat('%', lower(:q), '%'))
            """)
    Page<PurPurchaseOrderEntity> search(@Param("companyId") UUID companyId,
                                         @Param("state") PurchaseOrderState state,
                                         @Param("vendorId") UUID vendorPartnerId,
                                         @Param("q") String q,
                                         Pageable pageable);
}
