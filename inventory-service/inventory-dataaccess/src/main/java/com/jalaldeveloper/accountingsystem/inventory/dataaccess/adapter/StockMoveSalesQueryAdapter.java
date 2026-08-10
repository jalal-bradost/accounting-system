package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockPickingEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockMoveJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.StockPickingJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.MoveState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.StockMoveSalesQueryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
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
    public List<UUID> findPickingIdsBySalesOrderId(UUID salesOrderId) {
        return stockPickingJpaRepository.findBySalesOrderId(salesOrderId).stream()
                .map(StockPickingEntity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsNonTerminalPickingForSalesOrder(UUID salesOrderId) {
        return stockPickingJpaRepository.existsNonTerminalForSalesOrder(salesOrderId);
    }
}
