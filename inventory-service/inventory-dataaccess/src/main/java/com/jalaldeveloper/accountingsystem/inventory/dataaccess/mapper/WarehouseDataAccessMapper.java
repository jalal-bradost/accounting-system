package com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.StockLocationEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.WarehouseEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockLocation;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;
import org.springframework.stereotype.Component;

@Component
public class WarehouseDataAccessMapper {

    public Warehouse warehouseEntityToDomain(WarehouseEntity e) {
        if (e == null) return null;
        Warehouse.Builder b = Warehouse.builder()
                .id(new WarehouseId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .code(e.getCode())
                .name(e.getName())
                .stockLocationId(e.getStockLocationId() != null ? new StockLocationId(e.getStockLocationId()) : null)
                .inputLocationId(e.getInputLocationId() != null ? new StockLocationId(e.getInputLocationId()) : null)
                .outputLocationId(e.getOutputLocationId() != null ? new StockLocationId(e.getOutputLocationId()) : null);
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public WarehouseEntity warehouseDomainToEntity(Warehouse w, WarehouseEntity existingOrNull) {
        if (w == null) return null;
        WarehouseEntity e = existingOrNull != null ? existingOrNull : new WarehouseEntity();
        e.setId(w.getId().getId());
        e.setCompanyId(w.getCompanyId().getId());
        e.setCode(w.getCode());
        e.setName(w.getName());
        e.setStockLocationId(w.getStockLocationId() != null ? w.getStockLocationId().getId() : null);
        e.setInputLocationId(w.getInputLocationId() != null ? w.getInputLocationId().getId() : null);
        e.setOutputLocationId(w.getOutputLocationId() != null ? w.getOutputLocationId().getId() : null);
        e.setActive(w.isActive());
        e.setArchivedAt(w.getArchivedAt());
        e.setArchivedBy(w.getArchivedBy());
        return e;
    }

    public StockLocation locationEntityToDomain(StockLocationEntity e) {
        if (e == null) return null;
        StockLocation.Builder b = StockLocation.builder()
                .id(new StockLocationId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .code(e.getCode())
                .name(e.getName())
                .locationType(e.getLocationType())
                .parentId(e.getParentId() != null ? new StockLocationId(e.getParentId()) : null)
                .warehouseId(e.getWarehouseId() != null ? new WarehouseId(e.getWarehouseId()) : null)
                .allowNegativeStock(e.isAllowNegativeStock());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public StockLocationEntity locationDomainToEntity(StockLocation l, StockLocationEntity existingOrNull) {
        if (l == null) return null;
        StockLocationEntity e = existingOrNull != null ? existingOrNull : new StockLocationEntity();
        e.setId(l.getId().getId());
        e.setCompanyId(l.getCompanyId().getId());
        e.setCode(l.getCode());
        e.setName(l.getName());
        e.setLocationType(l.getLocationType());
        e.setParentId(l.getParentId() != null ? l.getParentId().getId() : null);
        e.setWarehouseId(l.getWarehouseId() != null ? l.getWarehouseId().getId() : null);
        e.setAllowNegativeStock(l.isAllowNegativeStock());
        e.setActive(l.isActive());
        e.setArchivedAt(l.getArchivedAt());
        e.setArchivedBy(l.getArchivedBy());
        return e;
    }
}
