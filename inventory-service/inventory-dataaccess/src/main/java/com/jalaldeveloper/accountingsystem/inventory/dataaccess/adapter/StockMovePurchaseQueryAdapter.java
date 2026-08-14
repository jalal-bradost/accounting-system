package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockPickingEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockMoveJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockPickingJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.MoveState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.StockMovePurchaseQueryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StockMovePurchaseQueryAdapter implements StockMovePurchaseQueryPort {

    private final StockMoveJpaRepository stockMoveJpaRepository;
    private final StockPickingJpaRepository stockPickingJpaRepository;

    public StockMovePurchaseQueryAdapter(StockMoveJpaRepository stockMoveJpaRepository,
                                         StockPickingJpaRepository stockPickingJpaRepository) {
        this.stockMoveJpaRepository = stockMoveJpaRepository;
        this.stockPickingJpaRepository = stockPickingJpaRepository;
    }

    @Override
    public BigDecimal sumPickedQuantityForPurchaseOrderLine(UUID purchaseOrderLineId) {
        BigDecimal incoming = stockMoveJpaRepository.sumPickedForPurchaseOrderLineAndType(
                purchaseOrderLineId, MoveState.DONE, PickingType.INCOMING);
        BigDecimal outgoing = stockMoveJpaRepository.sumPickedForPurchaseOrderLineAndType(
                purchaseOrderLineId, MoveState.DONE, PickingType.OUTGOING);
        BigDecimal in = incoming != null ? incoming : BigDecimal.ZERO;
        BigDecimal out = outgoing != null ? outgoing : BigDecimal.ZERO;
        return in.subtract(out);
    }

    @Override
    public List<UUID> findPickingIdsByPurchaseOrderId(UUID purchaseOrderId) {
        return stockPickingJpaRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(p -> p.getPickingType() == PickingType.INCOMING)
                .map(StockPickingEntity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsNonTerminalPickingForPurchaseOrder(UUID purchaseOrderId) {
        return stockPickingJpaRepository.existsNonTerminalForPurchaseOrder(purchaseOrderId);
    }
}
