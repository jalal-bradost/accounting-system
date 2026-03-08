package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import java.util.UUID;

public class AccountResponse {
    private final UUID id;
    private final UUID companyId;
    private final String code;
    private final String name;
    private final AccountType accountType;
    private final boolean active;

    public AccountResponse(UUID id, UUID companyId, String code, String name, AccountType accountType, boolean active) {
        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.accountType = accountType;
        this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public AccountType getAccountType() { return accountType; }
    public boolean isActive() { return active; }
}
