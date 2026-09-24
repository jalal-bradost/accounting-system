package com.bradox.erp.integration;

import com.bradox.erp.contacts.domain.core.valueobject.PartnerId;
import com.bradox.erp.contacts.service.domain.ports.output.accounting.PartnerBalancePort;
import com.bradox.erp.dataaccess.repository.CompanyCurrencyJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalItemJpaRepository;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.Money;
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
    public Money outstandingPayable(CompanyId companyId, PartnerId partnerId) {
        BigDecimal sum = journalItemRepository.sumPartnerBalanceByAccountType(
                companyId.getId(), partnerId.getId(), AccountType.PAYABLE);
        if (sum == null || sum.signum() == 0) {
            return Money.ZERO;
        }
        // Payable accounts are credit-normal; negate (debit - credit) so positive means amount owed.
        return new Money(sum.negate());
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
