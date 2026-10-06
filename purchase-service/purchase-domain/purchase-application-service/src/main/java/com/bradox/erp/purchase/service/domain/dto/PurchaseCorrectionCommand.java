package com.bradox.erp.purchase.service.domain.dto;

import com.bradox.erp.domain.valueobject.DiscountType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Input for the guided corrections of a confirmed purchase order. Each flow reads only the fields
 * it needs: change terms (line price/discount/taxes, order discount), reduce quantities (line qty),
 * reassign vendor (vendorPartnerId), cancel with documents (reason only).
 */
public class PurchaseCorrectionCommand {

    private List<Line> lines = new ArrayList<>();
    private DiscountType orderDiscountType;
    private BigDecimal orderDiscountValue;
    private UUID vendorPartnerId;
    private String reason;

    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
    public DiscountType getOrderDiscountType() { return orderDiscountType; }
    public void setOrderDiscountType(DiscountType orderDiscountType) { this.orderDiscountType = orderDiscountType; }
    public BigDecimal getOrderDiscountValue() { return orderDiscountValue; }
    public void setOrderDiscountValue(BigDecimal orderDiscountValue) { this.orderDiscountValue = orderDiscountValue; }
    public UUID getVendorPartnerId() { return vendorPartnerId; }
    public void setVendorPartnerId(UUID vendorPartnerId) { this.vendorPartnerId = vendorPartnerId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static class Line {
        private UUID purchaseOrderLineId;
        /** New ordered quantity (reduce quantities). */
        private BigDecimal qty;
        /** New unit price / discount / taxes (change terms); null keeps the current value. */
        private BigDecimal unitPrice;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private List<UUID> taxIds;

        public UUID getPurchaseOrderLineId() { return purchaseOrderLineId; }
        public void setPurchaseOrderLineId(UUID purchaseOrderLineId) { this.purchaseOrderLineId = purchaseOrderLineId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public DiscountType getDiscountType() { return discountType; }
        public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
        public BigDecimal getDiscountValue() { return discountValue; }
        public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
        public List<UUID> getTaxIds() { return taxIds; }
        public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds; }
    }
}
