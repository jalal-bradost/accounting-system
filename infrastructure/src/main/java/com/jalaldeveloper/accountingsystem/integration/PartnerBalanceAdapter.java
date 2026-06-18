package com.jalaldeveloper.accountingsystem.integration;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.accounting.PartnerBalancePort;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.CompanyCurrencyJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalItemJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Infrastructure-side adapter that fulfils the contacts module's {@link PartnerBalancePort}
 * by aggregating posted accounting journal items against the receivable account type
 * for the given partner. Lives in infrastructure to avoid a Maven-level dependency
 * from contacts to accounting.
 */
@Component
public class PartnerBalanceAdapter implements PartnerBalancePort {

    private final JournalItemJpaRepository journalItemRepository;
    private final CompanyCurrencyJpaRepository companyCurrencyJpaRepository;

    public PartnerBalanceAdapter(JournalItemJpaRepository journalItemRepository,
                                 CompanyCurrencyJpaRepository companyCurrencyJpaRepository) {
        this.journalItemRepository = journalItemRepository;
        this.companyCurrencyJpaRepository = companyCurrencyJpaRepository;
    }

    @Override
    public Money outstandingReceivable(CompanyId companyId, PartnerId partnerId) {
        BigDecimal sum = journalItemRepository.sumPartnerBalanceByAccountType(
                companyId.getId(), partnerId.getId(), AccountType.RECEIVABLE);
        return new Money(sum != null ? sum : BigDecimal.ZERO);
    }

    @Override
    public String companyBaseCurrencyCode(CompanyId companyId) {
        return companyCurrencyJpaRepository.findByCompanyIdOrderByBaseCurrencyDescCodeAsc(companyId.getId())
                .stream()
                .filter(c -> c.isBaseCurrency())
                .map(c -> c.getCode())
                .findFirst()
                .orElse("USD");
    }
}
