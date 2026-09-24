package com.bradox.erp.purchase.service.domain.dto;

import com.bradox.erp.domain.valueobject.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Updates a draft vendor bill or credit/debit note (header + existing lines).
 * Posted documents are immutable — use credit/debit notes instead.
 */
public class UpdateVendorBillCommand {

    private UUID companyId;
    @NotNull
    private LocalDate billDate;
    private LocalDate dueDate;
    private String reference;
    @PositiveOrZero
    private BigDecimal orderDiscountAmount;
    @Valid
    @NotNull
    private List<UpdateVendorBillLineCommand> lines = new ArrayList<>();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public BigDecimal getOrderDiscountAmount() { return orderDiscountAmount; }
    public void setOrderDiscountAmount(BigDecimal orderDiscountAmount) {
        this.orderDiscountAmount = orderDiscountAmount;
    }
    public List<UpdateVendorBillLineCommand> getLines() { return lines; }
    public void setLines(List<UpdateVendorBillLineCommand> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public static class UpdateVendorBillLineCommand {
        @NotNull
        private UUID lineId;
        @NotNull
        @Positive
        private BigDecimal qty;
        @NotNull
        @PositiveOrZero
        private BigDecimal unitPrice;
        private DiscountType discountType;
        @PositiveOrZero
        private BigDecimal discountValue;

        public UUID getLineId() { return lineId; }
        public void setLineId(UUID lineId) { this.lineId = lineId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public DiscountType getDiscountType() { return discountType; }
        public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
        public BigDecimal getDiscountValue() { return discountValue; }
        public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    }
}
