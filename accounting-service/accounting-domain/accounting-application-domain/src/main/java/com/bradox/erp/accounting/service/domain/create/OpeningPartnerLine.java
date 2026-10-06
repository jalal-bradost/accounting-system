package com.bradox.erp.accounting.service.domain.create;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Partner opening row. Positive amount creates an opening invoice/bill; negative creates an
 * opening unallocated payment (customer credit / vendor prepayment).
 */
public class OpeningPartnerLine {
    @NotNull
    private final UUID partnerId;
    @NotNull
    private final BigDecimal amount;
    private final String reference;
    private final LocalDate date;
    private final LocalDate dueDate;

    public OpeningPartnerLine(UUID partnerId, BigDecimal amount, String reference,
                              LocalDate date, LocalDate dueDate) {
        this.partnerId = partnerId;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.reference = reference;
        this.date = date;
        this.dueDate = dueDate;
    }

    public UUID getPartnerId() { return partnerId; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
    public LocalDate getDate() { return date; }
    public LocalDate getDueDate() { return dueDate; }
}
