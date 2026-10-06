package com.bradox.erp.inventory.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bulk opening-stock import: set target quantities with valuation to Opening Balance Equity. */
public class OpeningStockCommand {

    private UUID companyId;
    private UUID locationId;
    @NotEmpty
    @Valid
    private List<Line> lines = new ArrayList<>();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID locationId) { this.locationId = locationId; }
    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }

    public static class Line {
        private UUID productId;
        private String sku;
        @NotNull
        private BigDecimal quantity;
        private BigDecimal unitCost;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    }
}
