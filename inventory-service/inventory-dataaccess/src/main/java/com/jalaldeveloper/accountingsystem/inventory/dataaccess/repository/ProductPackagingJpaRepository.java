package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductPackagingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductPackagingJpaRepository extends JpaRepository<ProductPackagingEntity, UUID> {

    List<ProductPackagingEntity> findByProductIdOrderByBaseDescNameAsc(UUID productId);

    List<ProductPackagingEntity> findByProductIdInOrderByProductIdAscBaseDescNameAsc(Collection<UUID> productIds);

    Optional<ProductPackagingEntity> findByProductIdAndBaseTrue(UUID productId);

    @Query("""
        SELECT p FROM ProductPackagingEntity p
        WHERE p.companyId = :companyId
          AND p.active = TRUE
          AND p.barcode IS NOT NULL
          AND LOWER(TRIM(p.barcode)) = LOWER(TRIM(:barcode))
        """)
    Optional<ProductPackagingEntity> findActiveByCompanyIdAndBarcode(
            @Param("companyId") UUID companyId,
            @Param("barcode") String barcode);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProductPackagingEntity p
        WHERE p.companyId = :companyId
          AND p.productId = :productId
          AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
          AND (:excludeId IS NULL OR p.id <> :excludeId)
        """)
    boolean existsByProductIdAndNameExcludingId(
            @Param("companyId") UUID companyId,
            @Param("productId") UUID productId,
            @Param("name") String name,
            @Param("excludeId") UUID excludeId);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProductPackagingEntity p
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
