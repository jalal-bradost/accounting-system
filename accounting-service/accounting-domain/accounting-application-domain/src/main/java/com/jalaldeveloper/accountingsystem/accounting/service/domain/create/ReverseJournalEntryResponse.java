package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReverseJournalEntryResponse {
    @NotNull
    private final UUID originalJournalEntryId;
    @NotNull
    private final UUID reversalJournalEntryId;
    @NotNull
    private final String message;

    public ReverseJournalEntryResponse(UUID originalJournalEntryId, UUID reversalJournalEntryId, String message) {
        this.originalJournalEntryId = originalJournalEntryId;
        this.reversalJournalEntryId = reversalJournalEntryId;
        this.message = message;
    }

    public UUID getOriginalJournalEntryId() { return originalJournalEntryId; }
    public UUID getReversalJournalEntryId() { return reversalJournalEntryId; }
    public String getMessage() { return message; }
}
