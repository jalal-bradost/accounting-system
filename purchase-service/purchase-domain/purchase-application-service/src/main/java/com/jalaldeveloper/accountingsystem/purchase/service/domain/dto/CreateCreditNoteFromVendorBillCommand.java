package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateCreditNoteFromVendorBillCommand {

    private UUID companyId;
    @NotNull
    private LocalDate billDate;
    private LocalDate dueDate;
    private String reference;
    /** When empty, credit note uses full quantity of every source line. */
    @Valid
    private List<CreditNoteLineQtyCommand> lines = new ArrayList<>();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public List<CreditNoteLineQtyCommand> getLines() { return lines; }
    public void setLines(List<CreditNoteLineQtyCommand> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public static class CreditNoteLineQtyCommand {
        @NotNull
        private UUID billLineId;
        @NotNull
        @Positive
        private BigDecimal qty;

        public UUID getBillLineId() { return billLineId; }
        public void setBillLineId(UUID billLineId) { this.billLineId = billLineId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
    }
}
