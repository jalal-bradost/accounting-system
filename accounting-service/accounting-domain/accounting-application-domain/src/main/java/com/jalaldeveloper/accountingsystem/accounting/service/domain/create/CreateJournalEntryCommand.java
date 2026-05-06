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
    /** Optional: when omitted, server generates via sequence. */
    private final String sequenceNumber;
    @NotNull
    private final LocalDate date;
    private final String currencyCode;
    /** Optional partner attached at the entry level (e.g. customer/vendor for the document). */
    private final UUID partnerId;
    @NotEmpty
    @Valid
    private final List<JournalItemCommand> items;

    public CreateJournalEntryCommand(UUID companyId, UUID journalId, String sequenceNumber, LocalDate date,
                                     String currencyCode, List<JournalItemCommand> items) {
        this(companyId, journalId, sequenceNumber, date, currencyCode, null, items);
    }

    public CreateJournalEntryCommand(UUID companyId, UUID journalId, String sequenceNumber, LocalDate date,
                                     String currencyCode, UUID partnerId, List<JournalItemCommand> items) {
        this.companyId = companyId;
        this.journalId = journalId;
        this.sequenceNumber = sequenceNumber != null ? sequenceNumber : "";
        this.date = date;
        this.currencyCode = currencyCode;
        this.partnerId = partnerId;
        this.items = items != null ? items : List.of();
    }

    public UUID getCompanyId() { return companyId; }
    public UUID getJournalId() { return journalId; }
    public String getSequenceNumber() { return sequenceNumber; }
    public LocalDate getDate() { return date; }
    public String getCurrencyCode() { return currencyCode; }
    public UUID getPartnerId() { return partnerId; }
    public List<JournalItemCommand> getItems() { return items; }
}
