package com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockValuationLayerEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockMoveId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationLayerId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StockValuationLayerDataAccessMapper {

    public StockValuationLayer entityToDomain(StockValuationLayerEntity e) {
        if (e == null) return null;
        return StockValuationLayer.builder()
                .id(new ValuationLayerId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .productId(new ProductId(e.getProductId()))
                .stockMoveId(e.getStockMoveId() != null ? new StockMoveId(e.getStockMoveId()) : null)
                .method(e.getMethod())
                .occurredAt(e.getOccurredAt())
                .quantity(e.getQuantity())
                .unitCost(new Money(e.getUnitCost() != null ? e.getUnitCost() : BigDecimal.ZERO))
                .value(new Money(e.getValue() != null ? e.getValue() : BigDecimal.ZERO))
                .remainingQuantity(e.getRemainingQuantity())
                .remainingValue(new Money(e.getRemainingValue() != null ? e.getRemainingValue() : BigDecimal.ZERO))
                .journalEntryId(e.getJournalEntryId())
                .build();
    }

    public StockValuationLayerEntity domainToEntity(StockValuationLayer l, StockValuationLayerEntity existingOrNull) {
        if (l == null) return null;
        StockValuationLayerEntity e = existingOrNull != null ? existingOrNull : new StockValuationLayerEntity();
        e.setId(l.getId().getId());
        e.setCompanyId(l.getCompanyId().getId());
        e.setProductId(l.getProductId().getId());
        e.setStockMoveId(l.getStockMoveId() != null ? l.getStockMoveId().getId() : null);
        e.setMethod(l.getMethod());
        e.setOccurredAt(l.getOccurredAt());
        e.setQuantity(l.getQuantity());
        e.setUnitCost(l.getUnitCost() != null ? l.getUnitCost().getAmount() : BigDecimal.ZERO);
        e.setValue(l.getValue() != null ? l.getValue().getAmount() : BigDecimal.ZERO);
        e.setRemainingQuantity(l.getRemainingQuantity() != null ? l.getRemainingQuantity() : BigDecimal.ZERO);
        e.setRemainingValue(l.getRemainingValue() != null ? l.getRemainingValue().getAmount() : BigDecimal.ZERO);
        e.setJournalEntryId(l.getJournalEntryId());
        return e;
    }
}
