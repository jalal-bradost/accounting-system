package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import java.util.UUID;

public class JournalResponse {
    private final UUID id;
    private final UUID companyId;
    private final String code;
    private final String name;
    private final JournalType journalType;

    public JournalResponse(UUID id, UUID companyId, String code, String name, JournalType journalType) {
        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.journalType = journalType;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public JournalType getJournalType() { return journalType; }
}
