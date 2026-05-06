package com.jalaldeveloper.accountingsystem.integration;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.accounting.PartnerBalancePort;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalItemJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Container-side adapter that fulfils the contacts module's {@link PartnerBalancePort}
 * by aggregating posted accounting journal items against the receivable account type
 * for the given partner. Lives in the container to avoid a Maven-level dependency
 * from contacts to accounting.
 */
@Component
public class PartnerBalanceAdapter implements PartnerBalancePort {

    private final JournalItemJpaRepository journalItemRepository;

    public PartnerBalanceAdapter(JournalItemJpaRepository journalItemRepository) {
        this.journalItemRepository = journalItemRepository;
    }

    @Override
    public Money outstandingReceivable(CompanyId companyId, PartnerId partnerId) {
        BigDecimal sum = journalItemRepository.sumPartnerBalanceByAccountType(
                companyId.getId(), partnerId.getId(), AccountType.RECEIVABLE);
        return new Money(sum != null ? sum : BigDecimal.ZERO);
    }
}
