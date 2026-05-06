package com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contacts_partner", indexes = {
        @Index(name = "ix_contacts_partner_company", columnList = "company_id,active"),
        @Index(name = "ix_contacts_partner_company_name", columnList = "company_id,display_name")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("contacts.partner")
public class PartnerEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @AuditTrack
    private PartnerKind kind;

    @Column(name = "display_name", nullable = false, length = 255)
    @AuditTrack(name = "displayName")
    private String displayName;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "is_customer", nullable = false)
    @AuditTrack(name = "isCustomer")
    private boolean isCustomer;

    @Column(name = "is_vendor", nullable = false)
    @AuditTrack(name = "isVendor")
    private boolean isVendor;

    @Column(name = "credit_limit", precision = 19, scale = 4, nullable = false)
    @AuditTrack(name = "creditLimit")
    private BigDecimal creditLimit;

    @Column(name = "payment_terms_id")
    private UUID paymentTermsId;

    @Column(name = "receivable_account_id")
    private UUID receivableAccountId;

    @Column(name = "payable_account_id")
    private UUID payableAccountId;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(length = 10)
    private String language;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PartnerAddressEntity> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PartnerBankAccountEntity> bankAccounts = new ArrayList<>();

    public PartnerEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public PartnerKind getKind() { return kind; }
    public void setKind(PartnerKind kind) { this.kind = kind; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public boolean isCustomer() { return isCustomer; }
    public void setCustomer(boolean customer) { isCustomer = customer; }
    public boolean isVendor() { return isVendor; }
    public void setVendor(boolean vendor) { isVendor = vendor; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public UUID getPaymentTermsId() { return paymentTermsId; }
    public void setPaymentTermsId(UUID paymentTermsId) { this.paymentTermsId = paymentTermsId; }
    public UUID getReceivableAccountId() { return receivableAccountId; }
    public void setReceivableAccountId(UUID receivableAccountId) { this.receivableAccountId = receivableAccountId; }
    public UUID getPayableAccountId() { return payableAccountId; }
    public void setPayableAccountId(UUID payableAccountId) { this.payableAccountId = payableAccountId; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public List<PartnerAddressEntity> getAddresses() { return addresses; }
    public void setAddresses(List<PartnerAddressEntity> addresses) { this.addresses = addresses != null ? addresses : new ArrayList<>(); }
    public List<PartnerBankAccountEntity> getBankAccounts() { return bankAccounts; }
    public void setBankAccounts(List<PartnerBankAccountEntity> bankAccounts) { this.bankAccounts = bankAccounts != null ? bankAccounts : new ArrayList<>(); }
}
