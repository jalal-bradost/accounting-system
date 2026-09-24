package com.bradox.erp.inventory.domain.core.entity;

import com.bradox.erp.domain.entity.ArchivableAggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.exception.InventoryDomainException;
import com.bradox.erp.inventory.domain.core.valueobject.StockLocationId;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;

import java.time.Instant;

/**
 * Container for a set of locations (typically Stock + Input + Output) plus the picking-type
 * defaults for that warehouse. Inspired by Odoo's stock.warehouse.
 */
public class Warehouse extends ArchivableAggregateRoot<WarehouseId> {

    private final CompanyId companyId;
    private String code;
    private String name;

    private StockLocationId stockLocationId;
    private StockLocationId inputLocationId;
    private StockLocationId outputLocationId;

    private Warehouse(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.code = b.code;
        this.name = b.name;
        this.stockLocationId = b.stockLocationId;
        this.inputLocationId = b.inputLocationId;
        this.outputLocationId = b.outputLocationId;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("error.inventory.companyIdRequired", null, "companyId required");
        if (code == null || code.isBlank()) throw new InventoryDomainException("error.inventory.warehouseCodeRequired", null, "warehouse code required");
        if (name == null || name.isBlank()) throw new InventoryDomainException("error.inventory.warehouseNameRequired", null, "warehouse name required");
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new InventoryDomainException("error.inventory.warehouseNameRequired", null, "warehouse name required");
        this.name = name;
    }

    public void linkStockLocation(StockLocationId id) { this.stockLocationId = id; }
    public void linkInputLocation(StockLocationId id) { this.inputLocationId = id; }
    public void linkOutputLocation(StockLocationId id) { this.outputLocationId = id; }

    public CompanyId getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public StockLocationId getStockLocationId() { return stockLocationId; }
    public StockLocationId getInputLocationId() { return inputLocationId; }
    public StockLocationId getOutputLocationId() { return outputLocationId; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private WarehouseId id;
        private CompanyId companyId;
        private String code;
        private String name;
        private StockLocationId stockLocationId;
        private StockLocationId inputLocationId;
        private StockLocationId outputLocationId;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(WarehouseId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder code(String v) { this.code = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder stockLocationId(StockLocationId v) { this.stockLocationId = v; return this; }
        public Builder inputLocationId(StockLocationId v) { this.inputLocationId = v; return this; }
        public Builder outputLocationId(StockLocationId v) { this.outputLocationId = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public Warehouse build() { return new Warehouse(this); }
    }
}
