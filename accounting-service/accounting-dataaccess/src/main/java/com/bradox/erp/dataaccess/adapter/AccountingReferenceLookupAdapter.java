package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountRepository;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalRepository;
import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.bradox.erp.domain.core.entity.Journal;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountingReferenceLookupAdapter implements AccountingReferenceLookupPort {

    private final AccountRepository accountRepository;
    private final JournalRepository journalRepository;

    public AccountingReferenceLookupAdapter(AccountRepository accountRepository,
                                            JournalRepository journalRepository) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
    }

    @Override
    public UUID resolveAccountIdByCode(UUID companyId, String code) {
        return accountRepository.findByCompanyIdAndCode(new CompanyId(companyId), code)
                .map(account -> account.getId().getId())
                .orElseThrow(() -> new AccountingDomainException("Account code not found: " + code));
    }

    @Override
    public UUID resolveJournalIdByCode(UUID companyId, String code) {
        return journalRepository.findByCompanyIdAndCode(new CompanyId(companyId), code)
                .map(j -> j.getId().getId())
                .orElseThrow(() -> new AccountingDomainException("Journal code not found: " + code));
    }

    @Override
    public UUID resolveLiquidityAccountIdForJournal(UUID companyId, UUID journalId) {
        Journal journal = journalRepository.findById(new JournalId(journalId))
                .orElseThrow(() -> new AccountingDomainException("Payment journal not found"));
        if (!journal.getCompanyId().getId().equals(companyId)) {
            throw new AccountingDomainException("error.accounting.journalCompanyMismatch", null, "Journal company mismatch");
        }
        if (journal.getJournalType() != JournalType.CASH && journal.getJournalType() != JournalType.BANK) {
            throw new AccountingDomainException("error.accounting.paymentJournalCashOrBank", null, "Payment journal must be cash or bank");
        }
        return resolveAccountIdByCode(companyId, journal.getCode());
    }

    @Override
    public UUID resolveJournalIdByType(UUID companyId, JournalType journalType) {
        return journalRepository.findByCompanyId(new CompanyId(companyId)).stream()
                .filter(j -> j.getJournalType() == journalType)
                .findFirst()
                .map(j -> j.getId().getId())
                .orElseThrow(() -> new AccountingDomainException(
                        "Journal of type " + journalType + " not found"));
    }

    @Override
    public JournalType resolveJournalType(UUID companyId, UUID journalId) {
        Journal journal = journalRepository.findById(new JournalId(journalId))
                .orElseThrow(() -> new AccountingDomainException("Journal not found"));
        if (!journal.getCompanyId().getId().equals(companyId)) {
            throw new AccountingDomainException("error.accounting.journalCompanyMismatch", null, "Journal company mismatch");
        }
        return journal.getJournalType();
    }
}
