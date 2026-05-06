package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductCategoryJpaRepository extends JpaRepository<ProductCategoryEntity, UUID> {

    @Query("""
        SELECT c FROM ProductCategoryEntity c
        WHERE c.companyId = :companyId
          AND (:includeArchived = TRUE OR c.active = TRUE)
        ORDER BY c.name ASC
        """)
    List<ProductCategoryEntity> findByCompany(@Param("companyId") UUID companyId,
                                               @Param("includeArchived") boolean includeArchived);
}
