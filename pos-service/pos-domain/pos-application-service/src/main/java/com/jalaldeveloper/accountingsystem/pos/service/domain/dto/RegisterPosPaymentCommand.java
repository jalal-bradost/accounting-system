package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosPaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class RegisterPosPaymentCommand {
    @NotNull
    private PosPaymentMethod method;
    private UUID journalId;
    @NotNull
    @Positive
    private BigDecimal amount;
    private String reference;

    public PosPaymentMethod getMethod() { return method; }
    public void setMethod(PosPaymentMethod method) { this.method = method; }
    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
