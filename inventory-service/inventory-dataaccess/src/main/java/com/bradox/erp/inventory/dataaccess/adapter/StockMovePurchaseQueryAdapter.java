package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.inventory.dataaccess.entity.StockPickingEntity;
import com.bradox.erp.inventory.dataaccess.repository.StockMoveJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockPickingJpaRepository;
import com.bradox.erp.inventory.domain.core.valueobject.MoveState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingType;
import com.bradox.erp.inventory.service.domain.ports.output.StockMovePurchaseQueryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    public List<UUID> findReturnPickingIdsByPurchaseOrderId(UUID purchaseOrderId) {
        return stockPickingJpaRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(p -> p.getPickingType() == PickingType.OUTGOING)
                .map(StockPickingEntity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UUID> findReturnableReceiptPickingId(UUID purchaseOrderId) {
        return stockPickingJpaRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(p -> p.getPickingType() == PickingType.INCOMING)
                .filter(p -> p.getState() == PickingState.DONE)
                .sorted(Comparator.comparing(StockPickingEntity::getValidatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(StockPickingEntity::getId)
                .findFirst();
    }

    @Override
    public boolean existsNonTerminalPickingForPurchaseOrder(UUID purchaseOrderId) {
        return stockPickingJpaRepository.existsNonTerminalForPurchaseOrder(purchaseOrderId);
    }

    @Override
    public List<UUID> findNonTerminalPickingIdsByPurchaseOrderId(UUID purchaseOrderId) {
        return stockPickingJpaRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(p -> p.getState() != PickingState.DONE && p.getState() != PickingState.CANCELLED)
                .map(StockPickingEntity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsDonePickingForPurchaseOrder(UUID purchaseOrderId) {
        return stockPickingJpaRepository.existsDoneForPurchaseOrder(purchaseOrderId);
    }

    @Override
    public boolean isPickingToRefund(UUID pickingId) {
        if (pickingId == null) {
            return true;
        }
        return stockPickingJpaRepository.findById(pickingId)
                .map(StockPickingEntity::isToRefund)
                .orElse(true);
    }
}
