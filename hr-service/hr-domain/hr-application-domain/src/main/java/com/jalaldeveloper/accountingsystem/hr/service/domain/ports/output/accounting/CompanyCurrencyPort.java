package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.accounting;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

public interface CompanyCurrencyPort {

    boolean isActiveCurrency(CompanyId companyId, String currencyCode);

    String defaultCurrencyCode(CompanyId companyId);
}
