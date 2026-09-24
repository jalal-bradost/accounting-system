package com.bradox.erp.purchase.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates a debit note (extra vendor charge) linked to a posted vendor bill.
 * Increases accounts payable; JE direction matches a normal bill.
 */
public class CreateDebitNoteFromVendorBillCommand {

    private UUID companyId;
    @NotNull
    private LocalDate billDate;
    private LocalDate dueDate;
    private String reference;
    /** When empty, debit note copies full quantity of every source line (price adjust via edit). */
    @Valid
    private List<DebitNoteLineQtyCommand> lines = new ArrayList<>();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public List<DebitNoteLineQtyCommand> getLines() { return lines; }
    public void setLines(List<DebitNoteLineQtyCommand> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public static class DebitNoteLineQtyCommand {
        @NotNull
        private UUID billLineId;
        @NotNull
        @Positive
        private BigDecimal qty;
        /** Optional override unit price; when null, copies source line price. */
        private BigDecimal unitPrice;

        public UUID getBillLineId() { return billLineId; }
        public void setBillLineId(UUID billLineId) { this.billLineId = billLineId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}
