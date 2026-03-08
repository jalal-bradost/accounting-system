package com.jalaldeveloper.accountingsystem.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import com.jalaldeveloper.accountingsystem.domain.entity.BaseEntity;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

public class Journal extends BaseEntity<JournalId> {
    private final CompanyId companyId;
    private final String code;
    private final String name;
    private final JournalType journalType;

    private Journal(Builder builder) {
        super.setId(builder.id);
        companyId = builder.companyId;
        code = builder.code;
        name = builder.name;
        journalType = builder.journalType;
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public JournalType getJournalType() { return journalType; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private JournalId id;
        private CompanyId companyId;
        private String code;
        private String name;
        private JournalType journalType;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(JournalId val) {
            id = val;
            return this;
        }

        public Builder companyId(CompanyId val) {
            companyId = val;
            return this;
        }

        public Builder code(String val) {
            code = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder journalType(JournalType val) {
            journalType = val;
            return this;
        }

        public Journal build() {
            return new Journal(this);
        }
    }
}
