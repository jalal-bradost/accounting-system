package com.bradox.delin.inventory.dataaccess.repository;

import com.bradox.delin.inventory.dataaccess.entity.StockMoveEntity;
import com.bradox.delin.inventory.domain.core.valueobject.MoveState;
import com.bradox.delin.inventory.domain.core.valueobject.PickingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface StockMoveJpaRepository extends JpaRepository<StockMoveEntity, UUID> {

    @Query("""
            SELECT COALESCE(SUM(m.pickedQuantity), 0)
            FROM StockMoveEntity m
            JOIN m.picking p
            WHERE m.purchaseOrderLineId = :lineId
              AND m.state = :doneState
              AND p.pickingType = :pickingType
            """)
    BigDecimal sumPickedForPurchaseOrderLineAndType(@Param("lineId") UUID purchaseOrderLineId,
                                                    @Param("doneState") MoveState doneState,
                                                    @Param("pickingType") PickingType pickingType);

    @Query("""
            SELECT COALESCE(SUM(m.pickedQuantity), 0)
            FROM StockMoveEntity m
            JOIN m.picking p
            WHERE m.salesOrderLineId = :lineId
              AND m.state = :doneState
              AND p.pickingType = :pickingType
            """)
    BigDecimal sumPickedForSalesOrderLineAndType(@Param("lineId") UUID salesOrderLineId,
                                                 @Param("doneState") MoveState doneState,
                                                 @Param("pickingType") PickingType pickingType);

    @Query("""
            SELECT m
            FROM StockMoveEntity m
            JOIN FETCH m.picking p
            WHERE m.salesOrderLineId = :lineId
              AND m.state = :doneState
              AND p.pickingType = :pickingType
            """)
    List<StockMoveEntity> findDoneForSalesOrderLineAndType(@Param("lineId") UUID salesOrderLineId,
                                                           @Param("doneState") MoveState doneState,
                                                           @Param("pickingType") PickingType pickingType);

    boolean existsByProductId(UUID productId);
}
