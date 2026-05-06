package com.jalaldeveloper.accountingsystem.contacts.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PartnerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PartnerJpaRepository extends JpaRepository<PartnerEntity, UUID> {

    @Query("""
        SELECT p FROM PartnerEntity p
        WHERE p.companyId = :companyId
          AND (:isCustomer IS NULL OR p.isCustomer = :isCustomer)
          AND (:isVendor   IS NULL OR p.isVendor   = :isVendor)
          AND (:includeArchived = TRUE OR p.active = TRUE)
          AND (
                :query IS NULL OR :query = ''
                OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(p.legalName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(p.email, ''))    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(p.taxId, ''))    LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY p.displayName ASC
        """)
    Page<PartnerEntity> search(@Param("companyId") UUID companyId,
                               @Param("query") String query,
                               @Param("isCustomer") Boolean isCustomer,
                               @Param("isVendor") Boolean isVendor,
                               @Param("includeArchived") boolean includeArchived,
                               Pageable pageable);
}
