package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.FiscalTaxScope;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.TaxAmountType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateFiscalTaxCommand {

    private UUID companyId;
    @NotNull private String name;
    @NotNull private TaxAmountType amountType;
    @NotNull private BigDecimal amount;
    private boolean priceInclude;
    @NotNull private FiscalTaxScope scope;
    @NotNull private UUID accountId;
    private UUID refundAccountId;

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
}
