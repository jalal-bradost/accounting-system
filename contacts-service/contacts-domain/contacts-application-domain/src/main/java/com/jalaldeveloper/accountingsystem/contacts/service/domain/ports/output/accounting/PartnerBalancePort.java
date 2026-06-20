package com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.accounting;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;

/**
 * Output port that lets the contacts module read a partner's outstanding receivable
 * balance from accounting without depending on the accounting module at the Maven
 * level. Implemented by an adapter living in {@code accounting-container}.
 *
 * <p>Returns {@link Money#ZERO} when no balance is recorded.
 */
public interface PartnerBalancePort {

    Money outstandingReceivable(CompanyId companyId, PartnerId partnerId);

    Money outstandingPayable(CompanyId companyId, PartnerId partnerId);

    /** ISO code of the company's functional (base) currency for GL balances. */
    String companyBaseCurrencyCode(CompanyId companyId);
}
