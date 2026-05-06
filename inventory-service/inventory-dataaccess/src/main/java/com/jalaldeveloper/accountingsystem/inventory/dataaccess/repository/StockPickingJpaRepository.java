package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockPickingEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StockPickingJpaRepository extends JpaRepository<StockPickingEntity, UUID> {

    List<StockPickingEntity> findByPurchaseOrderId(UUID purchaseOrderId);

    List<StockPickingEntity> findBySalesOrderId(UUID salesOrderId);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM StockPickingEntity p
            WHERE p.purchaseOrderId = :poId
              AND p.state NOT IN ('DONE', 'CANCELLED')
            """)
    boolean existsNonTerminalForPurchaseOrder(@Param("poId") UUID purchaseOrderId);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM StockPickingEntity p
            WHERE p.salesOrderId = :soId
              AND p.state NOT IN ('DONE', 'CANCELLED')
            """)
    boolean existsNonTerminalForSalesOrder(@Param("soId") UUID salesOrderId);

    @Query("""
        SELECT p FROM StockPickingEntity p
        WHERE p.companyId = :companyId
          AND (:pickingType IS NULL OR p.pickingType = :pickingType)
          AND (:state        IS NULL OR p.state        = :state)
        ORDER BY COALESCE(p.scheduledAt, p.validatedAt) DESC, p.id ASC
        """)
    Page<StockPickingEntity> search(@Param("companyId") UUID companyId,
                                     @Param("pickingType") PickingType pickingType,
                                     @Param("state") PickingState state,
                                     Pageable pageable);
}
