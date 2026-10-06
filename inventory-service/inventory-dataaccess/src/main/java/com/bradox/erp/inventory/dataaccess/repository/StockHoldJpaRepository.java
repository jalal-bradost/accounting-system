package com.bradox.erp.inventory.dataaccess.repository;

import com.bradox.erp.inventory.dataaccess.entity.StockHoldEntity;
import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockHoldJpaRepository extends JpaRepository<StockHoldEntity, UUID> {

    @Query("""
        SELECT h FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.ownerType = :ownerType
          AND h.ownerId = :ownerId
        """)
    List<StockHoldEntity> findByOwner(@Param("companyId") UUID companyId,
                                      @Param("ownerType") StockHoldOwnerType ownerType,
                                      @Param("ownerId") UUID ownerId);

    @Query("""
        SELECT h FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.ownerType = :ownerType
          AND h.ownerId = :ownerId
          AND h.productId = :productId
        """)
    Optional<StockHoldEntity> findByOwnerAndProduct(@Param("companyId") UUID companyId,
                                                    @Param("ownerType") StockHoldOwnerType ownerType,
                                                    @Param("ownerId") UUID ownerId,
                                                    @Param("productId") UUID productId);

    @Query("""
        SELECT COALESCE(SUM(h.quantity), 0) FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.warehouseId = :warehouseId
          AND h.productId = :productId
          AND h.expiresAt > :now
          AND NOT (h.ownerType = :excludeOwnerType AND h.ownerId = :excludeOwnerId)
        """)
    BigDecimal sumActiveExcludingOwner(@Param("companyId") UUID companyId,
                                       @Param("warehouseId") UUID warehouseId,
                                       @Param("productId") UUID productId,
                                       @Param("now") Instant now,
                                       @Param("excludeOwnerType") StockHoldOwnerType excludeOwnerType,
                                       @Param("excludeOwnerId") UUID excludeOwnerId);

    @Query("""
        SELECT COALESCE(SUM(h.quantity), 0) FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.warehouseId = :warehouseId
          AND h.productId = :productId
          AND h.expiresAt > :now
        """)
    BigDecimal sumActive(@Param("companyId") UUID companyId,
                         @Param("warehouseId") UUID warehouseId,
                         @Param("productId") UUID productId,
                         @Param("now") Instant now);

    @Query("""
        SELECT h.productId, COALESCE(SUM(h.quantity), 0)
        FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.warehouseId = :warehouseId
          AND h.expiresAt > :now
          AND (:excludeOwnerType IS NULL OR NOT (h.ownerType = :excludeOwnerType AND h.ownerId = :excludeOwnerId))
        GROUP BY h.productId
        """)
    List<Object[]> sumActiveGroupedByProduct(@Param("companyId") UUID companyId,
                                             @Param("warehouseId") UUID warehouseId,
                                             @Param("now") Instant now,
                                             @Param("excludeOwnerType") StockHoldOwnerType excludeOwnerType,
                                             @Param("excludeOwnerId") UUID excludeOwnerId);

    @Query("""
        SELECT COALESCE(SUM(h.quantity), 0) FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.warehouseId = :warehouseId
          AND h.productId = :productId
          AND h.expiresAt > :now
          AND NOT (h.ownerType = :soType AND h.ownerId = :salesOrderId)
        """)
    BigDecimal sumActiveExceptSalesOrder(@Param("companyId") UUID companyId,
                                         @Param("warehouseId") UUID warehouseId,
                                         @Param("productId") UUID productId,
                                         @Param("now") Instant now,
                                         @Param("soType") StockHoldOwnerType soType,
                                         @Param("salesOrderId") UUID salesOrderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.ownerType = :ownerType
          AND h.ownerId = :ownerId
        """)
    int deleteByOwner(@Param("companyId") UUID companyId,
                      @Param("ownerType") StockHoldOwnerType ownerType,
                      @Param("ownerId") UUID ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StockHoldEntity h WHERE h.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);

    @Query("""
        SELECT h FROM StockHoldEntity h
        WHERE h.companyId = :companyId
          AND h.warehouseId = :warehouseId
          AND h.productId IN :productIds
          AND h.expiresAt > :now
        """)
    List<StockHoldEntity> findActiveForProducts(@Param("companyId") UUID companyId,
                                                @Param("warehouseId") UUID warehouseId,
                                                @Param("productIds") Collection<UUID> productIds,
                                                @Param("now") Instant now);
}
