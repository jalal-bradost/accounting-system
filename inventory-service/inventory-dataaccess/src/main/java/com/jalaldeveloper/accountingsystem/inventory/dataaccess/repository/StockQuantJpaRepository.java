package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockQuantEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
}
