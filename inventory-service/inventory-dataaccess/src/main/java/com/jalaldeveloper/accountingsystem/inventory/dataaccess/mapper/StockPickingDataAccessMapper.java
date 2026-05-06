package com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockMoveEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockPickingEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockMove;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockPicking;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockMoveId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockPickingId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class StockPickingDataAccessMapper {

    public StockPicking entityToDomain(StockPickingEntity e) {
        if (e == null) return null;
        List<StockMove> moves = new ArrayList<>();
        if (e.getMoves() != null) {
            for (StockMoveEntity m : e.getMoves()) {
                moves.add(moveEntityToDomain(m));
            }
        }
        return StockPicking.builder()
                .id(new StockPickingId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .warehouseId(e.getWarehouseId() != null ? new WarehouseId(e.getWarehouseId()) : null)
                .pickingType(e.getPickingType())
                .reference(e.getReference())
                .sourceLocationId(new StockLocationId(e.getSourceLocationId()))
                .destinationLocationId(new StockLocationId(e.getDestinationLocationId()))
                .partnerId(e.getPartnerId())
                .origin(e.getOrigin())
                .scheduledAt(e.getScheduledAt())
                .validatedAt(e.getValidatedAt())
                .validatedBy(e.getValidatedBy())
                .state(e.getState())
                .moves(moves)
                .backorderOf(e.getBackorderOf() != null ? new StockPickingId(e.getBackorderOf()) : null)
                .purchaseOrderId(e.getPurchaseOrderId())
                .salesOrderId(e.getSalesOrderId())
                .build();
    }

    private StockMove moveEntityToDomain(StockMoveEntity e) {
        return StockMove.builder()
                .id(new StockMoveId(e.getId()))
                .pickingId(e.getPicking() != null ? new StockPickingId(e.getPicking().getId()) : null)
                .productId(new ProductId(e.getProductId()))
                .uomId(new UomId(e.getUomId()))
                .sourceLocationId(new StockLocationId(e.getSourceLocationId()))
                .destinationLocationId(new StockLocationId(e.getDestinationLocationId()))
                .demandQuantity(e.getDemandQuantity())
                .reservedQuantity(e.getReservedQuantity())
                .pickedQuantity(e.getPickedQuantity())
                .unitCost(new Money(e.getUnitCost() != null ? e.getUnitCost() : BigDecimal.ZERO))
                .state(e.getState())
                .purchaseOrderLineId(e.getPurchaseOrderLineId())
                .salesOrderLineId(e.getSalesOrderLineId())
                .build();
    }

    public StockPickingEntity domainToEntity(StockPicking p, StockPickingEntity existingOrNull) {
        if (p == null) return null;
        StockPickingEntity e = existingOrNull != null ? existingOrNull : new StockPickingEntity();
        e.setId(p.getId().getId());
        e.setCompanyId(p.getCompanyId().getId());
        e.setWarehouseId(p.getWarehouseId() != null ? p.getWarehouseId().getId() : null);
        e.setPickingType(p.getPickingType());
        e.setReference(p.getReference());
        e.setSourceLocationId(p.getSourceLocationId().getId());
        e.setDestinationLocationId(p.getDestinationLocationId().getId());
        e.setPartnerId(p.getPartnerId());
        e.setOrigin(p.getOrigin());
        e.setScheduledAt(p.getScheduledAt());
        e.setValidatedAt(p.getValidatedAt());
        e.setValidatedBy(p.getValidatedBy());
        e.setState(p.getState());
        e.setBackorderOf(p.getBackorderOf() != null ? p.getBackorderOf().getId() : null);
        e.setPurchaseOrderId(p.getPurchaseOrderId());
        e.setSalesOrderId(p.getSalesOrderId());

        if (e.getMoves() == null) e.setMoves(new ArrayList<>());
        Map<UUID, StockMoveEntity> existing = new HashMap<>();
        for (StockMoveEntity m : e.getMoves()) existing.put(m.getId(), m);
        e.getMoves().clear();
        for (StockMove m : p.getMoves()) {
            UUID id = m.getId() != null ? m.getId().getId() : UUID.randomUUID();
            StockMoveEntity moveEntity = existing.getOrDefault(id, new StockMoveEntity());
            moveEntity.setId(id);
            moveEntity.setPicking(e);
            moveEntity.setProductId(m.getProductId().getId());
            moveEntity.setUomId(m.getUomId().getId());
            moveEntity.setSourceLocationId(m.getSourceLocationId().getId());
            moveEntity.setDestinationLocationId(m.getDestinationLocationId().getId());
            moveEntity.setDemandQuantity(m.getDemandQuantity());
            moveEntity.setReservedQuantity(m.getReservedQuantity());
            moveEntity.setPickedQuantity(m.getPickedQuantity());
            moveEntity.setUnitCost(m.getUnitCost() != null ? m.getUnitCost().getAmount() : BigDecimal.ZERO);
            moveEntity.setState(m.getState());
            moveEntity.setPurchaseOrderLineId(m.getPurchaseOrderLineId());
            moveEntity.setSalesOrderLineId(m.getSalesOrderLineId());
            e.getMoves().add(moveEntity);
        }
        return e;
    }
}
