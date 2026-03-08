package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateJournalResponse {
    @NotNull
    private final UUID journalId;
    @NotNull
    private final String message;

    public CreateJournalResponse(UUID journalId, String message) {
        this.journalId = journalId;
        this.message = message;
    }

    public UUID getJournalId() { return journalId; }
    public String getMessage() { return message; }
}
