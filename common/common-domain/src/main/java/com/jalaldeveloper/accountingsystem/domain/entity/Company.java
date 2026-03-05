package com.jalaldeveloper.accountingsystem.domain.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;

public class Company extends AggregateRoot<CompanyId> {
    private final String name;
    private final Currency baseCurrency; // Everyone needs to know the currency
    private final String countryCode;

    private Company(Builder builder) {
        super.setId(builder.id);
        name = builder.name;
        baseCurrency = builder.baseCurrency;
        countryCode = builder.countryCode;
    }


    public static final class Builder {
        private CompanyId id;
        private String name;
        private Currency baseCurrency;
        private String countryCode;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(CompanyId val) {
            id = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder baseCurrency(Currency val) {
            baseCurrency = val;
            return this;
        }

        public Builder countryCode(String val) {
            countryCode = val;
            return this;
        }

        public Company build() {
            return new Company(this);
        }
    }
}
