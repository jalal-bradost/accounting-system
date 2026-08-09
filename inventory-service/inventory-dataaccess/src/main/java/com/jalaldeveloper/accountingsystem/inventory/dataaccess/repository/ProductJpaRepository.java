package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.companyId = :companyId
          AND (:includeArchived = TRUE OR p.active = TRUE)
          AND (
                :query IS NULL OR :query = ''
                OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(p.barcode, '')) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY p.name ASC
        """)
    Page<ProductEntity> search(@Param("companyId") UUID companyId,
                                @Param("query") String query,
                                @Param("includeArchived") boolean includeArchived,
                                Pageable pageable);

    boolean existsByCategoryId(UUID categoryId);
}
