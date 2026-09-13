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

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.companyId = :companyId
          AND p.active = TRUE
          AND p.barcode IS NOT NULL
          AND LOWER(TRIM(p.barcode)) = LOWER(TRIM(:barcode))
        """)
    java.util.Optional<ProductEntity> findActiveByCompanyIdAndBarcode(
            @Param("companyId") UUID companyId,
            @Param("barcode") String barcode);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.companyId = :companyId
          AND p.active = TRUE
          AND LOWER(TRIM(p.sku)) = LOWER(TRIM(:sku))
        """)
    java.util.Optional<ProductEntity> findActiveByCompanyIdAndSku(
            @Param("companyId") UUID companyId,
            @Param("sku") String sku);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProductEntity p
        WHERE p.companyId = :companyId
          AND p.barcode IS NOT NULL
          AND LOWER(TRIM(p.barcode)) = LOWER(TRIM(:barcode))
          AND (:excludeId IS NULL OR p.id <> :excludeId)
        """)
    boolean existsByCompanyIdAndBarcodeExcludingId(
            @Param("companyId") UUID companyId,
            @Param("barcode") String barcode,
            @Param("excludeId") UUID excludeId);
}
