package com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurFiscalTaxEntity;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.FiscalTax;
import org.springframework.stereotype.Component;

@Component
public class FiscalTaxDataAccessMapper {

    public FiscalTax entityToDomain(PurFiscalTaxEntity e) {
        if (e == null) return null;
        FiscalTax d = new FiscalTax();
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setName(e.getName());
        d.setAmountType(e.getAmountType());
        d.setAmount(e.getAmount());
        d.setPriceInclude(e.isPriceInclude());
        d.setScope(e.getScope());
        d.setAccountId(e.getAccountId());
        d.setRefundAccountId(e.getRefundAccountId());
        d.setActive(e.isActive());
        return d;
    }

    public PurFiscalTaxEntity domainToEntity(FiscalTax d, PurFiscalTaxEntity existingOrNull) {
        if (d == null) return null;
        PurFiscalTaxEntity e = existingOrNull != null ? existingOrNull : new PurFiscalTaxEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setName(d.getName());
        e.setAmountType(d.getAmountType());
        e.setAmount(d.getAmount());
        e.setPriceInclude(d.isPriceInclude());
        e.setScope(d.getScope());
        e.setAccountId(d.getAccountId());
        e.setRefundAccountId(d.getRefundAccountId());
        e.setActive(d.isActive());
        return e;
    }
}
