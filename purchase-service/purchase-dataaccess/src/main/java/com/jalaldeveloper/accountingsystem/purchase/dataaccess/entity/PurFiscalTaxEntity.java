package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.FiscalTaxScope;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.TaxAmountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pur_fiscal_tax", indexes = {
        @Index(name = "ix_pur_tax_company", columnList = "company_id,active")
})
public class PurFiscalTaxEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_type", nullable = false, length = 20)
    private TaxAmountType amountType;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "price_include", nullable = false)
    private boolean priceInclude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FiscalTaxScope scope;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "refund_account_id")
    private UUID refundAccountId;

    @Column(nullable = false)
    private boolean active;

    public PurFiscalTaxEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TaxAmountType getAmountType() { return amountType; }
    public void setAmountType(TaxAmountType amountType) { this.amountType = amountType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public boolean isPriceInclude() { return priceInclude; }
    public void setPriceInclude(boolean priceInclude) { this.priceInclude = priceInclude; }
    public FiscalTaxScope getScope() { return scope; }
    public void setScope(FiscalTaxScope scope) { this.scope = scope; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getRefundAccountId() { return refundAccountId; }
    public void setRefundAccountId(UUID refundAccountId) { this.refundAccountId = refundAccountId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
