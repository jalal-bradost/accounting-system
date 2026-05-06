package com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockQuantEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockQuant;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockQuantId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StockQuantDataAccessMapper {

    public StockQuant entityToDomain(StockQuantEntity e) {
        if (e == null) return null;
        return StockQuant.builder()
                .id(new StockQuantId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .productId(new ProductId(e.getProductId()))
                .locationId(new StockLocationId(e.getLocationId()))
                .quantity(e.getQuantity() != null ? e.getQuantity() : BigDecimal.ZERO)
                .reservedQuantity(e.getReservedQuantity() != null ? e.getReservedQuantity() : BigDecimal.ZERO)
                .lastChangedAt(e.getLastChangedAt())
                .version(e.getVersion())
                .build();
    }

    public StockQuantEntity domainToEntity(StockQuant q, StockQuantEntity existingOrNull) {
        if (q == null) return null;
        StockQuantEntity e = existingOrNull != null ? existingOrNull : new StockQuantEntity();
        e.setId(q.getId().getId());
        e.setCompanyId(q.getCompanyId().getId());
        e.setProductId(q.getProductId().getId());
        e.setLocationId(q.getLocationId().getId());
        e.setQuantity(q.getQuantity() != null ? q.getQuantity() : BigDecimal.ZERO);
        e.setReservedQuantity(q.getReservedQuantity() != null ? q.getReservedQuantity() : BigDecimal.ZERO);
        e.setLastChangedAt(q.getLastChangedAt());
        return e;
    }
}
