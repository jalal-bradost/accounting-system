package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReverseJournalEntryCommand {
    @NotNull
    private final UUID journalEntryId;
    @NotBlank
    private final String reason;

    public ReverseJournalEntryCommand(UUID journalEntryId, String reason) {
        this.journalEntryId = journalEntryId;
        this.reason = reason != null ? reason : "";
    }

    public UUID getJournalEntryId() { return journalEntryId; }
    public String getReason() { return reason; }
}
