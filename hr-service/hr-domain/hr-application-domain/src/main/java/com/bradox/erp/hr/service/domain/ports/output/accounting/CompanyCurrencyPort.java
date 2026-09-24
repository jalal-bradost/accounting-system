package com.bradox.erp.hr.service.domain.ports.output.accounting;

import com.bradox.erp.domain.valueobject.CompanyId;

public interface CompanyCurrencyPort {

    boolean isActiveCurrency(CompanyId companyId, String currencyCode);

    String defaultCurrencyCode(CompanyId companyId);
}
