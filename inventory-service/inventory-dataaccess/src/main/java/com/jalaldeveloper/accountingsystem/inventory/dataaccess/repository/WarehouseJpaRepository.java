package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WarehouseJpaRepository extends JpaRepository<WarehouseEntity, UUID> {

    @Query("""
        SELECT w FROM WarehouseEntity w
        WHERE w.companyId = :companyId
          AND (:includeArchived = TRUE OR w.active = TRUE)
        ORDER BY w.code ASC
        """)
    List<WarehouseEntity> findByCompany(@Param("companyId") UUID companyId,
                                         @Param("includeArchived") boolean includeArchived);
}
