package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class VendorBillLineResponse {
    private UUID id;
    private int sequence;
    private UUID purchaseOrderLineId;
    private UUID productId;
    private String name;
    private UUID uomId;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private UUID accountId;
    private List<VendorBillLineTaxResponse> taxes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public UUID getPurchaseOrderLineId() { return purchaseOrderLineId; }
    public void setPurchaseOrderLineId(UUID purchaseOrderLineId) { this.purchaseOrderLineId = purchaseOrderLineId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public List<VendorBillLineTaxResponse> getTaxes() { return taxes; }
    public void setTaxes(List<VendorBillLineTaxResponse> taxes) { this.taxes = taxes; }
}
