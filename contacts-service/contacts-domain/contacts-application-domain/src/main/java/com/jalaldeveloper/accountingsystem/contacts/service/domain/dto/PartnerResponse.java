package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.AddressType;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PartnerResponse {

    private final UUID id;
    private final UUID companyId;
    private final PartnerKind kind;
    private final String displayName;
    private final String legalName;
    private final UUID parentId;
    private final boolean isCustomer;
    private final boolean isVendor;
    private final BigDecimal creditLimit;
    private final UUID paymentTermsId;
    private final UUID receivableAccountId;
    private final UUID payableAccountId;
    private final String taxId;
    private final String email;
    private final String phone;
    private final String website;
    private final String language;
    private final String currencyCode;
    private final boolean active;
    private final Instant archivedAt;
    private final String archivedBy;
    private final List<AddressResponse> addresses;
    private final List<BankAccountResponse> bankAccounts;

    public PartnerResponse(UUID id, UUID companyId, PartnerKind kind, String displayName, String legalName,
                           UUID parentId, boolean isCustomer, boolean isVendor, BigDecimal creditLimit,
                           UUID paymentTermsId, UUID receivableAccountId, UUID payableAccountId,
                           String taxId, String email, String phone, String website, String language,
                           String currencyCode, boolean active, Instant archivedAt, String archivedBy,
                           List<AddressResponse> addresses, List<BankAccountResponse> bankAccounts) {
        this.id = id;
        this.companyId = companyId;
        this.kind = kind;
        this.displayName = displayName;
        this.legalName = legalName;
        this.parentId = parentId;
        this.isCustomer = isCustomer;
        this.isVendor = isVendor;
        this.creditLimit = creditLimit;
        this.paymentTermsId = paymentTermsId;
        this.receivableAccountId = receivableAccountId;
        this.payableAccountId = payableAccountId;
        this.taxId = taxId;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.language = language;
        this.currencyCode = currencyCode;
        this.active = active;
        this.archivedAt = archivedAt;
        this.archivedBy = archivedBy;
        this.addresses = addresses;
        this.bankAccounts = bankAccounts;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public PartnerKind getKind() { return kind; }
    public String getDisplayName() { return displayName; }
    public String getLegalName() { return legalName; }
    public UUID getParentId() { return parentId; }

    @JsonProperty("isCustomer")
    public boolean isCustomer() {
        return isCustomer;
    }

    @JsonProperty("isVendor")
    public boolean isVendor() {
        return isVendor;
    }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public UUID getPaymentTermsId() { return paymentTermsId; }
    public UUID getReceivableAccountId() { return receivableAccountId; }
    public UUID getPayableAccountId() { return payableAccountId; }
    public String getTaxId() { return taxId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getWebsite() { return website; }
    public String getLanguage() { return language; }
    public String getCurrencyCode() { return currencyCode; }
    public boolean isActive() { return active; }
    public Instant getArchivedAt() { return archivedAt; }
    public String getArchivedBy() { return archivedBy; }
    public List<AddressResponse> getAddresses() { return addresses; }
    public List<BankAccountResponse> getBankAccounts() { return bankAccounts; }

    public record AddressResponse(UUID id, AddressType type, boolean defaultForType,
                                   String street1, String street2, String city,
                                   String state, String postalCode, String country) {}

    public record BankAccountResponse(UUID id, String iban, String swift, String accountHolderName) {}
}
