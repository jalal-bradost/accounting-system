package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateJournalEntryResponse {
    @NotNull
    private final UUID journalEntryId;
    @NotNull
    private final String message;

    public CreateJournalEntryResponse(UUID journalEntryId, String message) {
        this.journalEntryId = journalEntryId;
        this.message = message;
    }

    public UUID getJournalEntryId() { return journalEntryId; }
    public String getMessage() { return message; }
}
