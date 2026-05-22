package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CreatePartnerCommand {

    /** Optional: when omitted, resolved from CompanyContext (X-Company-Id header). */
    private UUID companyId;

    @NotNull private PartnerKind kind;
    @NotBlank private String displayName;
    private String legalName;
    private UUID parentId;

    /**
     * Explicit JSON names: JavaBean {@code isCustomer()}/{@code isVendor()} otherwise map to
     * {@code customer}/{@code vendor} in JSON, so the frontend's {@code isCustomer}/{@code isVendor}
     * payload was ignored and both stayed false.
     */
    @JsonProperty("isCustomer")
    private boolean isCustomer;

    @JsonProperty("isVendor")
    private boolean isVendor;

    private BigDecimal creditLimit;
    private UUID paymentTermsId;
    private UUID receivableAccountId;
    private UUID payableAccountId;

    private String taxId;
    @Email private String email;
    private String phone;
    private String website;
    private String language;
    private String currencyCode;

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
}
