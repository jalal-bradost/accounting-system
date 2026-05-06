package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StockLocationJpaRepository extends JpaRepository<StockLocationEntity, UUID> {

    @Query("""
        SELECT l FROM StockLocationEntity l
        WHERE l.companyId = :companyId
          AND (:includeArchived = TRUE OR l.active = TRUE)
        ORDER BY l.code ASC
        """)
    List<StockLocationEntity> findByCompany(@Param("companyId") UUID companyId,
                                             @Param("includeArchived") boolean includeArchived);

    @Query("""
        SELECT l FROM StockLocationEntity l
        WHERE l.warehouseId = :warehouseId
          AND (:includeArchived = TRUE OR l.active = TRUE)
        ORDER BY l.code ASC
        """)
    List<StockLocationEntity> findByWarehouse(@Param("warehouseId") UUID warehouseId,
                                               @Param("includeArchived") boolean includeArchived);
}
