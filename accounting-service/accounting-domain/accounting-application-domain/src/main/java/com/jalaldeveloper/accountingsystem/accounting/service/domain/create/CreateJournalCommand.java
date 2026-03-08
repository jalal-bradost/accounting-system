package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateJournalCommand {
    @NotNull
    private final UUID companyId;
    @NotNull
    private final String code;
    @NotNull
    private final String name;
    @NotNull
    private final JournalType journalType;

    public CreateJournalCommand(UUID companyId, String code, String name, JournalType journalType) {
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.journalType = journalType;
    }

    public UUID getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public JournalType getJournalType() { return journalType; }
}
