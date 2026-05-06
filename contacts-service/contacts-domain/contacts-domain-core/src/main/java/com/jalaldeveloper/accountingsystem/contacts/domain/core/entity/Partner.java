package com.jalaldeveloper.accountingsystem.contacts.domain.core.entity;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.exception.ContactsDomainException;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.AddressType;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Unified partner aggregate inspired by Odoo's res.partner. A single record represents
 * a contact that can be a customer, vendor, both, or neither (e.g. an internal
 * employee). Owns its addresses and bank accounts.
 *
 * <p>Mutating operations on the aggregate go through behavior methods (e.g. {@link
 * #setCustomer(boolean)}, {@link #addAddress(PartnerAddress)}) so domain invariants are
 * always enforced.
 */
public class Partner extends ArchivableAggregateRoot<PartnerId> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CompanyId companyId;
    private PartnerKind kind;
    private String displayName;
    private String legalName;
    private PartnerId parentId;

    private boolean isCustomer;
    private boolean isVendor;

    private Money creditLimit;
    private PaymentTermsId paymentTermsId;
    private UUID receivableAccountId;
    private UUID payableAccountId;

    private String taxId;
    private String email;
    private String phone;
    private String website;
    private String language;
    private Currency currency;

    private final List<PartnerAddress> addresses = new ArrayList<>();
    private final List<PartnerBankAccount> bankAccounts = new ArrayList<>();

    private Partner(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.kind = b.kind;
        this.displayName = b.displayName;
        this.legalName = b.legalName;
        this.parentId = b.parentId;
        this.isCustomer = b.isCustomer;
        this.isVendor = b.isVendor;
        this.creditLimit = b.creditLimit != null ? b.creditLimit : Money.ZERO;
        this.paymentTermsId = b.paymentTermsId;
        this.receivableAccountId = b.receivableAccountId;
        this.payableAccountId = b.payableAccountId;
        this.taxId = b.taxId;
        this.email = b.email;
        this.phone = b.phone;
        this.website = b.website;
        this.language = b.language;
        this.currency = b.currency;
        if (b.addresses != null) this.addresses.addAll(b.addresses);
        if (b.bankAccounts != null) this.bankAccounts.addAll(b.bankAccounts);
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new ContactsDomainException("companyId required");
        if (kind == null) throw new ContactsDomainException("kind required (COMPANY or INDIVIDUAL)");
        if (displayName == null || displayName.isBlank()) {
            throw new ContactsDomainException("displayName required");
        }
        if (creditLimit == null || creditLimit.getAmount().signum() < 0) {
            throw new ContactsDomainException("creditLimit must be >= 0");
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ContactsDomainException("email is not a valid address: " + email);
        }
    }

    /** Enforced when a parent reference is changed; needs the loaded parent for the kind check. */
    public void validateParent(Partner parent) {
        if (parentId == null) return;
        if (parent == null) {
            throw new ContactsDomainException("parent partner not found: " + parentId.getId());
        }
        if (parent.kind == PartnerKind.INDIVIDUAL && this.kind == PartnerKind.COMPANY) {
            throw new ContactsDomainException(
                    "A company partner cannot have an individual as parent (got " + parent.getId().getId() + ")");
        }
        if (Objects.equals(parent.getId(), this.getId())) {
            throw new ContactsDomainException("Partner cannot be its own parent");
        }
    }

    public void setCustomer(boolean isCustomer) { this.isCustomer = isCustomer; }
    public void setVendor(boolean isVendor) { this.isVendor = isVendor; }

    public void changeCreditLimit(Money newLimit) {
        if (newLimit == null || newLimit.getAmount().signum() < 0) {
            throw new ContactsDomainException("creditLimit must be >= 0");
        }
        this.creditLimit = newLimit;
    }

    public void changePaymentTerms(PaymentTermsId paymentTermsId) {
        this.paymentTermsId = paymentTermsId;
    }

    public void changeReceivableAccount(UUID accountId) { this.receivableAccountId = accountId; }
    public void changePayableAccount(UUID accountId) { this.payableAccountId = accountId; }

    public void rename(String displayName, String legalName) {
        if (displayName == null || displayName.isBlank()) {
            throw new ContactsDomainException("displayName required");
        }
        this.displayName = displayName;
        this.legalName = legalName;
    }

    public void changeContact(String email, String phone, String website) {
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ContactsDomainException("email is not a valid address: " + email);
        }
        this.email = email;
        this.phone = phone;
        this.website = website;
    }

    public void changeTaxId(String taxId) { this.taxId = taxId; }
    public void changeLanguage(String language) { this.language = language; }
    public void changeCurrency(Currency currency) { this.currency = currency; }
    public void changeKind(PartnerKind kind) {
        if (kind == null) throw new ContactsDomainException("kind required");
        this.kind = kind;
    }
    public void changeParent(PartnerId parentId) { this.parentId = parentId; }

    public PartnerAddress addAddress(PartnerAddress address) {
        if (address == null) throw new ContactsDomainException("address required");
        if (!Objects.equals(address.getPartnerId(), this.getId())) {
            throw new ContactsDomainException("address.partnerId does not match");
        }
        if (address.isDefaultForType()) {
            unmarkOtherDefaults(address.getType(), address.getId() != null ? address.getId().getId() : null);
        }
        addresses.add(address);
        return address;
    }

    public void removeAddress(UUID addressId) {
        addresses.removeIf(a -> a.getId() != null && a.getId().getId().equals(addressId));
    }

    public PartnerAddress findAddress(UUID addressId) {
        return addresses.stream()
                .filter(a -> a.getId() != null && a.getId().getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ContactsDomainException("address not found: " + addressId));
    }

    public void markAddressDefault(UUID addressId) {
        PartnerAddress address = findAddress(addressId);
        unmarkOtherDefaults(address.getType(), addressId);
        address.markDefault(true);
    }

    private void unmarkOtherDefaults(AddressType type, UUID exceptId) {
        addresses.stream()
                .filter(a -> a.getType() == type)
                .filter(a -> a.getId() == null || !a.getId().getId().equals(exceptId))
                .forEach(a -> a.markDefault(false));
    }

    public PartnerBankAccount addBankAccount(PartnerBankAccount account) {
        if (account == null) throw new ContactsDomainException("bank account required");
        if (!Objects.equals(account.getPartnerId(), this.getId())) {
            throw new ContactsDomainException("bankAccount.partnerId does not match");
        }
        bankAccounts.add(account);
        return account;
    }

    public void removeBankAccount(UUID bankAccountId) {
        bankAccounts.removeIf(b -> b.getId() != null && b.getId().getId().equals(bankAccountId));
    }

    public CompanyId getCompanyId() { return companyId; }
    public PartnerKind getKind() { return kind; }
    public String getDisplayName() { return displayName; }
    public String getLegalName() { return legalName; }
    public PartnerId getParentId() { return parentId; }
    public boolean isCustomer() { return isCustomer; }
    public boolean isVendor() { return isVendor; }
    public Money getCreditLimit() { return creditLimit; }
    public PaymentTermsId getPaymentTermsId() { return paymentTermsId; }
    public UUID getReceivableAccountId() { return receivableAccountId; }
    public UUID getPayableAccountId() { return payableAccountId; }
    public String getTaxId() { return taxId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getWebsite() { return website; }
    public String getLanguage() { return language; }
    public Currency getCurrency() { return currency; }
    public List<PartnerAddress> getAddresses() { return List.copyOf(addresses); }
    public List<PartnerBankAccount> getBankAccounts() { return List.copyOf(bankAccounts); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PartnerId id;
        private CompanyId companyId;
        private PartnerKind kind;
        private String displayName;
        private String legalName;
        private PartnerId parentId;
        private boolean isCustomer;
        private boolean isVendor;
        private Money creditLimit;
        private PaymentTermsId paymentTermsId;
        private UUID receivableAccountId;
        private UUID payableAccountId;
        private String taxId;
        private String email;
        private String phone;
        private String website;
        private String language;
        private Currency currency;
        private List<PartnerAddress> addresses;
        private List<PartnerBankAccount> bankAccounts;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(PartnerId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder kind(PartnerKind v) { this.kind = v; return this; }
        public Builder displayName(String v) { this.displayName = v; return this; }
        public Builder legalName(String v) { this.legalName = v; return this; }
        public Builder parentId(PartnerId v) { this.parentId = v; return this; }
        public Builder isCustomer(boolean v) { this.isCustomer = v; return this; }
        public Builder isVendor(boolean v) { this.isVendor = v; return this; }
        public Builder creditLimit(Money v) { this.creditLimit = v; return this; }
        public Builder paymentTermsId(PaymentTermsId v) { this.paymentTermsId = v; return this; }
        public Builder receivableAccountId(UUID v) { this.receivableAccountId = v; return this; }
        public Builder payableAccountId(UUID v) { this.payableAccountId = v; return this; }
        public Builder taxId(String v) { this.taxId = v; return this; }
        public Builder email(String v) { this.email = v; return this; }
        public Builder phone(String v) { this.phone = v; return this; }
        public Builder website(String v) { this.website = v; return this; }
        public Builder language(String v) { this.language = v; return this; }
        public Builder currency(Currency v) { this.currency = v; return this; }
        public Builder addresses(List<PartnerAddress> v) { this.addresses = v; return this; }
        public Builder bankAccounts(List<PartnerBankAccount> v) { this.bankAccounts = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public Partner build() { return new Partner(this); }
    }
}
