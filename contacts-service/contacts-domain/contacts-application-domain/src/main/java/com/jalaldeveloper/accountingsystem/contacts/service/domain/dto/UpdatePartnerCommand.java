package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;
import jakarta.validation.constraints.Email;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Patch-style update payload. Null fields are treated as "leave unchanged"; primitive
 * booleans use the explicit {@link Boolean} wrapper to keep the same semantics.
 */
public class UpdatePartnerCommand {

    private PartnerKind kind;
    private String displayName;
    private String legalName;
    private UUID parentId;
    private Boolean parentIdReset;

    private Boolean isCustomer;
    private Boolean isVendor;

    private BigDecimal creditLimit;
    private UUID paymentTermsId;
    private Boolean paymentTermsIdReset;
    private UUID receivableAccountId;
    private Boolean receivableAccountIdReset;
    private UUID payableAccountId;
    private Boolean payableAccountIdReset;

    private String taxId;
    @Email private String email;
    private String phone;
    private String website;
    private String language;
    private String currencyCode;

    public PartnerKind getKind() { return kind; }
    public void setKind(PartnerKind kind) { this.kind = kind; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public Boolean getParentIdReset() { return parentIdReset; }
    public void setParentIdReset(Boolean v) { this.parentIdReset = v; }
    public Boolean getIsCustomer() { return isCustomer; }
    public void setIsCustomer(Boolean v) { this.isCustomer = v; }
    public Boolean getIsVendor() { return isVendor; }
    public void setIsVendor(Boolean v) { this.isVendor = v; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal v) { this.creditLimit = v; }
    public UUID getPaymentTermsId() { return paymentTermsId; }
    public void setPaymentTermsId(UUID v) { this.paymentTermsId = v; }
    public Boolean getPaymentTermsIdReset() { return paymentTermsIdReset; }
    public void setPaymentTermsIdReset(Boolean v) { this.paymentTermsIdReset = v; }
    public UUID getReceivableAccountId() { return receivableAccountId; }
    public void setReceivableAccountId(UUID v) { this.receivableAccountId = v; }
    public Boolean getReceivableAccountIdReset() { return receivableAccountIdReset; }
    public void setReceivableAccountIdReset(Boolean v) { this.receivableAccountIdReset = v; }
    public UUID getPayableAccountId() { return payableAccountId; }
    public void setPayableAccountId(UUID v) { this.payableAccountId = v; }
    public Boolean getPayableAccountIdReset() { return payableAccountIdReset; }
    public void setPayableAccountIdReset(Boolean v) { this.payableAccountIdReset = v; }
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
}
