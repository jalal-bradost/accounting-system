package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateAccountResponse {
    @NotNull
    private final UUID accountId;
    @NotNull
    private final String message;

    public CreateAccountResponse(UUID accountId, String message) {
        this.accountId = accountId;
        this.message = message;
    }

    public UUID getAccountId() { return accountId; }
    public String getMessage() { return message; }
}
