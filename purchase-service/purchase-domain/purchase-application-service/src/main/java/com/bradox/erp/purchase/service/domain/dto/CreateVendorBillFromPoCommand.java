package com.bradox.erp.purchase.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class CreateVendorBillFromPoCommand {

    private UUID companyId;
    @NotNull private UUID purchaseOrderId;
    @NotNull private LocalDate billDate;
    private LocalDate dueDate;
    private String reference;
    /** When set, bill exactly these quantities (per order line) instead of what is billable. */
    private Map<UUID, BigDecimal> lineQuantities;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(UUID purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Map<UUID, BigDecimal> getLineQuantities() { return lineQuantities; }
    public void setLineQuantities(Map<UUID, BigDecimal> lineQuantities) { this.lineQuantities = lineQuantities; }
}
