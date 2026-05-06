package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;

import java.time.Instant;

/**
 * A node in the stock location tree (mirrors stock.location). Only {@link
 * LocationType#INTERNAL} (and PRODUCTION/TRANSIT) locations contribute to on-hand counts;
 * SUPPLIER, CUSTOMER, INVENTORY_LOSS are virtual counterparties used to balance moves.
 *
 * <p>Locations may belong to a warehouse (typical for INTERNAL) or be free-floating (typical
 * for SUPPLIER/CUSTOMER which are shared across warehouses).
 */
public class StockLocation extends ArchivableAggregateRoot<StockLocationId> {

    private final CompanyId companyId;
    private String code;
    private String name;
    private LocationType locationType;
    private StockLocationId parentId;
    private WarehouseId warehouseId;
    private boolean allowNegativeStock;

    private StockLocation(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.code = b.code;
        this.name = b.name;
        this.locationType = b.locationType;
        this.parentId = b.parentId;
        this.warehouseId = b.warehouseId;
        this.allowNegativeStock = b.allowNegativeStock;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (code == null || code.isBlank()) throw new InventoryDomainException("location code required");
        if (name == null || name.isBlank()) throw new InventoryDomainException("location name required");
        if (locationType == null) throw new InventoryDomainException("locationType required");
    }

    /** Internal locations contribute to on-hand qty; everything else is virtual / counterparty. */
    public boolean isInternal() {
        return locationType == LocationType.INTERNAL;
    }

    public boolean isExternal() {
        return locationType == LocationType.SUPPLIER || locationType == LocationType.CUSTOMER;
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new InventoryDomainException("location name required");
        this.name = name;
    }

    public void changeAllowNegative(boolean v) { this.allowNegativeStock = v; }
    public void changeWarehouse(WarehouseId warehouseId) { this.warehouseId = warehouseId; }
    public void changeParent(StockLocationId parentId) { this.parentId = parentId; }

    public CompanyId getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public LocationType getLocationType() { return locationType; }
    public StockLocationId getParentId() { return parentId; }
    public WarehouseId getWarehouseId() { return warehouseId; }
    public boolean isAllowNegativeStock() { return allowNegativeStock; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private StockLocationId id;
        private CompanyId companyId;
        private String code;
        private String name;
        private LocationType locationType;
        private StockLocationId parentId;
        private WarehouseId warehouseId;
        private boolean allowNegativeStock;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(StockLocationId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder code(String v) { this.code = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder locationType(LocationType v) { this.locationType = v; return this; }
        public Builder parentId(StockLocationId v) { this.parentId = v; return this; }
        public Builder warehouseId(WarehouseId v) { this.warehouseId = v; return this; }
        public Builder allowNegativeStock(boolean v) { this.allowNegativeStock = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public StockLocation build() { return new StockLocation(this); }
    }
}
