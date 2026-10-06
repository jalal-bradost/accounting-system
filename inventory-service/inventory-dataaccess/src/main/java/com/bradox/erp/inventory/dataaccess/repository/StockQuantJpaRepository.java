package com.bradox.erp.inventory.dataaccess.repository;

import com.bradox.erp.inventory.dataaccess.entity.StockQuantEntity;
import com.bradox.erp.inventory.domain.core.valueobject.LocationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockQuantJpaRepository extends JpaRepository<StockQuantEntity, UUID> {

    @Query("""
        SELECT q FROM StockQuantEntity q
        WHERE q.companyId = :companyId
          AND q.productId = :productId
          AND q.locationId = :locationId
        """)
    Optional<StockQuantEntity> findByProductLocation(@Param("companyId") UUID companyId,
                                                      @Param("productId") UUID productId,
                                                      @Param("locationId") UUID locationId);

    @Query("""
        SELECT COALESCE(SUM(q.quantity), 0) FROM StockQuantEntity q
        JOIN StockLocationEntity l ON l.id = q.locationId
        WHERE q.companyId = :companyId
          AND q.productId = :productId
          AND l.locationType = :internal
        """)
    BigDecimal sumOnHandInternal(@Param("companyId") UUID companyId,
                                  @Param("productId") UUID productId,
                                  @Param("internal") LocationType internal);

    @Query("""
        SELECT q.productId, COALESCE(SUM(q.quantity), 0) FROM StockQuantEntity q
        JOIN StockLocationEntity l ON l.id = q.locationId
        WHERE q.companyId = :companyId
          AND q.productId in :productIds
          AND l.locationType = :internal
        GROUP BY q.productId
        """)
    List<Object[]> sumOnHandInternalByProductIds(@Param("companyId") UUID companyId,
                                                   @Param("productIds") Collection<UUID> productIds,
                                                   @Param("internal") LocationType internal);

    @Query("""
        SELECT COALESCE(SUM(q.quantity), 0) FROM StockQuantEntity q
        JOIN StockLocationEntity l ON l.id = q.locationId
        WHERE q.companyId = :companyId
          AND q.productId = :productId
          AND l.warehouseId = :warehouseId
          AND l.locationType = :internal
        """)
    BigDecimal sumOnHandByWarehouse(@Param("companyId") UUID companyId,
                                    @Param("productId") UUID productId,
                                    @Param("warehouseId") UUID warehouseId,
                                    @Param("internal") LocationType internal);

    @Query("""
        SELECT q.productId, COALESCE(SUM(q.quantity), 0)
        FROM StockQuantEntity q
        JOIN StockLocationEntity l ON l.id = q.locationId
        WHERE q.companyId = :companyId
          AND l.warehouseId = :warehouseId
          AND l.locationType = :internal
        GROUP BY q.productId
        """)
    List<Object[]> sumOnHandGroupedByWarehouse(@Param("companyId") UUID companyId,
                                               @Param("warehouseId") UUID warehouseId,
                                               @Param("internal") LocationType internal);

    @Query("""
        SELECT q FROM StockQuantEntity q
        WHERE q.companyId = :companyId
          AND q.productId = :productId
        ORDER BY q.locationId ASC
        """)
    List<StockQuantEntity> findByProduct(@Param("companyId") UUID companyId,
                                          @Param("productId") UUID productId);

    @Query("""
        SELECT q FROM StockQuantEntity q
        WHERE q.companyId = :companyId
          AND q.locationId = :locationId
        ORDER BY q.productId ASC
        """)
    List<StockQuantEntity> findByLocation(@Param("companyId") UUID companyId,
                                           @Param("locationId") UUID locationId);

    boolean existsByProductId(UUID productId);

    /** Free-to-reserve qty (on-hand − hard reserved) at warehouse internal locations. */
    @Query("""
        SELECT COALESCE(SUM(q.quantity - q.reservedQuantity), 0) FROM StockQuantEntity q
        JOIN StockLocationEntity l ON l.id = q.locationId
        WHERE q.companyId = :companyId
          AND q.productId = :productId
          AND l.warehouseId = :warehouseId
          AND l.locationType = :internal
        """)
    BigDecimal sumFreeByWarehouse(@Param("companyId") UUID companyId,
                                  @Param("productId") UUID productId,
                                  @Param("warehouseId") UUID warehouseId,
                                  @Param("internal") LocationType internal);

    @Query("""
        SELECT q.productId, COALESCE(SUM(q.quantity), 0), COALESCE(SUM(q.reservedQuantity), 0)
        FROM StockQuantEntity q
        JOIN StockLocationEntity l ON l.id = q.locationId
        WHERE q.companyId = :companyId
          AND l.warehouseId = :warehouseId
          AND l.locationType = :internal
        GROUP BY q.productId
        """)
    List<Object[]> sumOnHandAndReservedGroupedByWarehouse(@Param("companyId") UUID companyId,
                                                          @Param("warehouseId") UUID warehouseId,
                                                          @Param("internal") LocationType internal);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT q FROM StockQuantEntity q
        JOIN StockLocationEntity l ON l.id = q.locationId
        WHERE q.companyId = :companyId
          AND q.productId IN :productIds
          AND l.warehouseId = :warehouseId
          AND l.locationType = :internal
        """)
    List<StockQuantEntity> lockByWarehouseProducts(@Param("companyId") UUID companyId,
                                                   @Param("warehouseId") UUID warehouseId,
                                                   @Param("productIds") Collection<UUID> productIds,
                                                   @Param("internal") LocationType internal);
}
