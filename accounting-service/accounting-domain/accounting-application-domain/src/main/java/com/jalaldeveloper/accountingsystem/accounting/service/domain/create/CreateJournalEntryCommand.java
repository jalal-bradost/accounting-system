package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreateJournalEntryCommand {
    @NotNull
    private final UUID companyId;
    @NotNull
    private final UUID journalId;
    @NotNull
    private final String sequenceNumber;
    @NotNull
    private final LocalDate date;
    private final String currencyCode;
    @NotEmpty
    @Valid
    private final List<JournalItemCommand> items;

    public CreateJournalEntryCommand(UUID companyId, UUID journalId, String sequenceNumber, LocalDate date,
                                     String currencyCode, List<JournalItemCommand> items) {
        this.companyId = companyId;
        this.journalId = journalId;
        this.sequenceNumber = sequenceNumber;
        this.date = date;
        this.currencyCode = currencyCode;
        this.items = items != null ? items : List.of();
    }

    public UUID getCompanyId() { return companyId; }
    public UUID getJournalId() { return journalId; }
    public String getSequenceNumber() { return sequenceNumber; }
    public LocalDate getDate() { return date; }
    public String getCurrencyCode() { return currencyCode; }
    public List<JournalItemCommand> getItems() { return items; }
}
