package com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateCreditNoteFromInvoiceCommand {

    private UUID companyId;
    @NotNull
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String reference;
    /** When empty, credit note uses full quantity of every source line. */
    @Valid
    private List<CreditNoteLineQtyCommand> lines = new ArrayList<>();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public List<CreditNoteLineQtyCommand> getLines() { return lines; }
    public void setLines(List<CreditNoteLineQtyCommand> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }

    public static class CreditNoteLineQtyCommand {
        @NotNull
        private UUID invoiceLineId;
        @NotNull
        @Positive
        private BigDecimal qty;

        public UUID getInvoiceLineId() { return invoiceLineId; }
        public void setInvoiceLineId(UUID invoiceLineId) { this.invoiceLineId = invoiceLineId; }
        public BigDecimal getQty() { return qty; }
        public void setQty(BigDecimal qty) { this.qty = qty; }
    }
}
