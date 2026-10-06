package com.bradox.erp.inventory.dataaccess.repository;

import com.bradox.erp.inventory.dataaccess.entity.StockHoldLockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StockHoldLockJpaRepository extends JpaRepository<StockHoldLockEntity, StockHoldLockEntity.Pk> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l FROM StockHoldLockEntity l
        WHERE l.companyId = :companyId
          AND l.warehouseId = :warehouseId
          AND l.productId = :productId
        """)
    Optional<StockHoldLockEntity> findForUpdate(@Param("companyId") UUID companyId,
                                                @Param("warehouseId") UUID warehouseId,
                                                @Param("productId") UUID productId);
}
