package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.inventory.dataaccess.entity.StockMoveEntity;
import com.bradox.erp.inventory.dataaccess.entity.StockPickingEntity;
import com.bradox.erp.inventory.dataaccess.repository.StockMoveJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockPickingJpaRepository;
import com.bradox.erp.inventory.domain.core.valueobject.MoveState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingType;
import com.bradox.erp.inventory.service.domain.ports.output.StockMoveSalesQueryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StockMoveSalesQueryAdapter implements StockMoveSalesQueryPort {

    private final StockMoveJpaRepository stockMoveJpaRepository;
    private final StockPickingJpaRepository stockPickingJpaRepository;

    public StockMoveSalesQueryAdapter(StockMoveJpaRepository stockMoveJpaRepository,
                                      StockPickingJpaRepository stockPickingJpaRepository) {
        this.stockMoveJpaRepository = stockMoveJpaRepository;
        this.stockPickingJpaRepository = stockPickingJpaRepository;
    }

    @Override
    public BigDecimal sumPickedQuantityForSalesOrderLine(UUID salesOrderLineId) {
        BigDecimal outgoing = stockMoveJpaRepository.sumPickedForSalesOrderLineAndType(
                salesOrderLineId, MoveState.DONE, PickingType.OUTGOING);
        BigDecimal incoming = stockMoveJpaRepository.sumPickedForSalesOrderLineAndType(
                salesOrderLineId, MoveState.DONE, PickingType.INCOMING);
        BigDecimal out = outgoing != null ? outgoing : BigDecimal.ZERO;
        BigDecimal in = incoming != null ? incoming : BigDecimal.ZERO;
        return out.subtract(in);
    }

    @Override
    public Optional<NetDeliveredCost> netDeliveredCostForSalesOrderLine(UUID salesOrderLineId) {
        return deliveredCost(salesOrderLineId, true);
    }

    @Override
    public Optional<NetDeliveredCost> grossDeliveredCostForSalesOrderLine(UUID salesOrderLineId) {
        return deliveredCost(salesOrderLineId, false);
    }

    private Optional<NetDeliveredCost> deliveredCost(UUID salesOrderLineId, boolean netOfReturns) {
        if (salesOrderLineId == null) {
            return Optional.empty();
        }
        List<StockMoveEntity> outgoing = stockMoveJpaRepository.findDoneForSalesOrderLineAndType(
                salesOrderLineId, MoveState.DONE, PickingType.OUTGOING);
        List<StockMoveEntity> incoming = netOfReturns
                ? stockMoveJpaRepository.findDoneForSalesOrderLineAndType(
                        salesOrderLineId, MoveState.DONE, PickingType.INCOMING)
                : List.of();
        if (outgoing.isEmpty() && incoming.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;
        UUID productId = null;
        for (StockMoveEntity m : outgoing) {
            productId = m.getProductId();
            BigDecimal picked = nz(m.getPickedQuantity());
            BigDecimal unit = nz(m.getUnitCost());
            qty = qty.add(picked);
            value = value.add(picked.multiply(unit));
        }
        for (StockMoveEntity m : incoming) {
            if (productId == null) {
                productId = m.getProductId();
            }
            BigDecimal picked = nz(m.getPickedQuantity());
            BigDecimal unit = nz(m.getUnitCost());
            qty = qty.subtract(picked);
            value = value.subtract(picked.multiply(unit));
        }
        if (productId == null || qty.signum() <= 0 || value.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new NetDeliveredCost(
                productId,
                qty.setScale(4, RoundingMode.HALF_UP),
                value.setScale(4, RoundingMode.HALF_UP)));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    public List<UUID> findPickingIdsBySalesOrderId(UUID salesOrderId) {
        return stockPickingJpaRepository.findBySalesOrderId(salesOrderId).stream()
                .filter(p -> p.getPickingType() == PickingType.OUTGOING)
                .map(StockPickingEntity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<UUID> findReturnPickingIdsBySalesOrderId(UUID salesOrderId) {
        return stockPickingJpaRepository.findBySalesOrderId(salesOrderId).stream()
                .filter(p -> p.getPickingType() == PickingType.INCOMING)
                .map(StockPickingEntity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UUID> findReturnableDeliveryPickingId(UUID salesOrderId) {
        return stockPickingJpaRepository.findBySalesOrderId(salesOrderId).stream()
                .filter(p -> p.getPickingType() == PickingType.OUTGOING)
                .filter(p -> p.getState() == PickingState.DONE)
                .sorted(Comparator.comparing(StockPickingEntity::getValidatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(StockPickingEntity::getId)
                .findFirst();
    }

    @Override
    public boolean existsNonTerminalPickingForSalesOrder(UUID salesOrderId) {
        return stockPickingJpaRepository.existsNonTerminalForSalesOrder(salesOrderId);
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
