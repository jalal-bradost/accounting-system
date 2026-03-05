package com.jalaldeveloper.accountingsystem.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.entity.AggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

public class Account extends AggregateRoot<AccountId> {
    private final CompanyId companyId;
    private final String code;
    private final String name;
    private final AccountType accountType;
    private final boolean active;

    private Account(Builder builder) {
        super.setId(builder.id);
        companyId = builder.companyId;
        code = builder.code;
        name = builder.name;
        accountType = builder.accountType;
        active = builder.active;
    }


    public static final class Builder {
        private AccountId id;
        private CompanyId companyId;
        private String code;
        private String name;
        private AccountType accountType;
        private boolean active;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(AccountId val) {
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

        public Builder accountType(AccountType val) {
            accountType = val;
            return this;
        }

        public Builder active(boolean val) {
            active = val;
            return this;
        }

        public Account build() {
            return new Account(this);
        }
    }
}
