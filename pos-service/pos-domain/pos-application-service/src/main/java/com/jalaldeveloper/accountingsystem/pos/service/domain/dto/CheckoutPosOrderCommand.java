package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckoutPosOrderCommand {
    private UUID companyId;
    @NotNull
    private UUID sessionId;
    private UUID customerPartnerId;
    private String note;
    private LocalDate businessDate;
    @NotEmpty
    @Valid
    private List<PosOrderLineCommand> lines = new ArrayList<>();
    @NotEmpty
    @Valid
    private List<RegisterPosPaymentCommand> payments = new ArrayList<>();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public List<PosOrderLineCommand> getLines() { return lines; }
    public void setLines(List<PosOrderLineCommand> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
    public List<RegisterPosPaymentCommand> getPayments() { return payments; }
    public void setPayments(List<RegisterPosPaymentCommand> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
    }
}
