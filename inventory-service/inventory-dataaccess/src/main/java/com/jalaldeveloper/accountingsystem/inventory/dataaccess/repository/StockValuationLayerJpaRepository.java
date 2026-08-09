package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockValuationLayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface StockValuationLayerJpaRepository extends JpaRepository<StockValuationLayerEntity, UUID> {

    @Query("""
        SELECT l FROM StockValuationLayerEntity l
        WHERE l.companyId = :companyId
          AND l.productId = :productId
          AND l.quantity > 0
          AND l.remainingQuantity > 0
        ORDER BY l.occurredAt ASC
        """)
    List<StockValuationLayerEntity> findFifoCandidates(@Param("companyId") UUID companyId,
                                                        @Param("productId") UUID productId);

    /**
     * Sums net valuation across all layers (positive receipts and negative deliveries) so the
     * answer reflects the current on-hand value regardless of valuation method (AVCO does not
     * decrement {@code remainingValue} per layer; FIFO does, but its negative-delivery layers
     * still net out the consumed receipts here).
     */
    @Query("""
        SELECT COALESCE(SUM(l.value), 0) FROM StockValuationLayerEntity l
        WHERE l.companyId = :companyId
          AND l.productId = :productId
        """)
    BigDecimal sumOnHandValue(@Param("companyId") UUID companyId,
                               @Param("productId") UUID productId);

    @Query("""
        SELECT l FROM StockValuationLayerEntity l
        WHERE l.companyId = :companyId
          AND l.productId = :productId
        ORDER BY l.occurredAt ASC
        """)
    List<StockValuationLayerEntity> findByProduct(@Param("companyId") UUID companyId,
                                                   @Param("productId") UUID productId);

    boolean existsByProductId(UUID productId);
}
