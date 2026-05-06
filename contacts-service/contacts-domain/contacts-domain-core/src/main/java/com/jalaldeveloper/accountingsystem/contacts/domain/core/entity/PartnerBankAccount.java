package com.jalaldeveloper.accountingsystem.contacts.domain.core.entity;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerBankAccountId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.domain.entity.BaseEntity;

public class PartnerBankAccount extends BaseEntity<PartnerBankAccountId> {

    private final PartnerId partnerId;
    private final String iban;
    private final String swift;
    private final String accountHolderName;

    private PartnerBankAccount(Builder b) {
        super.setId(b.id);
        this.partnerId = b.partnerId;
        this.iban = b.iban;
        this.swift = b.swift;
        this.accountHolderName = b.accountHolderName;
    }

    public PartnerId getPartnerId() { return partnerId; }
    public String getIban() { return iban; }
    public String getSwift() { return swift; }
    public String getAccountHolderName() { return accountHolderName; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PartnerBankAccountId id;
        private PartnerId partnerId;
        private String iban;
        private String swift;
        private String accountHolderName;

        public Builder id(PartnerBankAccountId v) { this.id = v; return this; }
        public Builder partnerId(PartnerId v) { this.partnerId = v; return this; }
        public Builder iban(String v) { this.iban = v; return this; }
        public Builder swift(String v) { this.swift = v; return this; }
        public Builder accountHolderName(String v) { this.accountHolderName = v; return this; }
        public PartnerBankAccount build() { return new PartnerBankAccount(this); }
    }
}
