package com.bradox.delin.hr.service.domain.ports.output.accounting;

import com.bradox.delin.domain.valueobject.CompanyId;

public interface CompanyCurrencyPort {

    boolean isActiveCurrency(CompanyId companyId, String currencyCode);

    String defaultCurrencyCode(CompanyId companyId);
}
