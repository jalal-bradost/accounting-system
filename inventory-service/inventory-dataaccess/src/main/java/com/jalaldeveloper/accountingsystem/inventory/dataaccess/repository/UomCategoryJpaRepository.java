package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UomCategoryJpaRepository extends JpaRepository<UomCategoryEntity, UUID> {

    @Query("""
        SELECT c FROM UomCategoryEntity c
        WHERE c.companyId = :companyId
          AND (:includeArchived = TRUE OR c.active = TRUE)
        ORDER BY c.name ASC
        """)
    List<UomCategoryEntity> findByCompany(@Param("companyId") UUID companyId,
                                           @Param("includeArchived") boolean includeArchived);
}
