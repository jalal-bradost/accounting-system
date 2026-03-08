package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateAccountCommand {
    @NotNull
    private final UUID companyId;
    @NotNull
    private final String code;
    @NotNull
    private final String name;
    @NotNull
    private final AccountType accountType;
    private final boolean active;

    public CreateAccountCommand(UUID companyId, String code, String name, AccountType accountType, boolean active) {
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.accountType = accountType;
        this.active = active;
    }

    public UUID getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public AccountType getAccountType() { return accountType; }
    public boolean isActive() { return active; }
}
