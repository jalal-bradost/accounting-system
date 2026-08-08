package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.OpeningBalanceApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Account;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class OpeningBalanceApplicationServiceImpl implements OpeningBalanceApplicationService {

    private static final String OPENING_JOURNAL_CODE = "OPEN";
    private static final String OPENING_BALANCE_EQUITY_ACCOUNT_CODE = "430019";
    private static final String LINE_LABEL = "Opening balance";

    private final AccountRepository accountRepository;
    private final JournalRepository journalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryApplicationService journalEntryApplicationService;

    OpeningBalanceApplicationServiceImpl(AccountRepository accountRepository,
                                         JournalRepository journalRepository,
                                         JournalEntryRepository journalEntryRepository,
                                         JournalEntryApplicationService journalEntryApplicationService) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryApplicationService = journalEntryApplicationService;
    }

    @Override
    @Transactional
    public OpeningBalanceResponse setOpeningBalances(OpeningBalanceCommand command) {
        CompanyId companyId = new CompanyId(command.getCompanyId());

        Journal openingJournal = journalRepository.findByCompanyIdAndCode(companyId, OPENING_JOURNAL_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "Opening journal (code " + OPENING_JOURNAL_CODE + ") not found for company."));
        Account obeAccount = accountRepository.findByCompanyIdAndCode(companyId, OPENING_BALANCE_EQUITY_ACCOUNT_CODE)
                .orElseThrow(() -> new AccountingDomainException(
                        "Opening Balance Equity account (code " + OPENING_BALANCE_EQUITY_ACCOUNT_CODE + ") not found for company."));

        UUID openingJournalId = openingJournal.getId().getId();
        handleExistingOpeningEntries(companyId, openingJournalId, command.isReplace());

        List<JournalItemCommand> items = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (OpeningBalanceLine line : command.getLines()) {
            BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Account account = accountRepository.findById(new AccountId(line.getAccountId()))
                    .orElseThrow(() -> new AccountingDomainException("Account not found: " + line.getAccountId()));
            boolean trade = isTradeAccount(account.getAccountType());
            if (trade && line.getPartnerId() == null) {
                throw new AccountingDomainException(
                        "A partner is required for receivable/payable opening balance on account " + account.getCode() + ".");
            }
            if (!trade && line.getPartnerId() != null) {
                throw new AccountingDomainException(
                        "A partner may only be set on receivable/payable opening balances (account " + account.getCode() + ").");
            }

            BigDecimal debit = amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ZERO;
            BigDecimal credit = amount.compareTo(BigDecimal.ZERO) < 0 ? amount.negate() : BigDecimal.ZERO;
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            items.add(new JournalItemCommand(line.getAccountId(), LINE_LABEL, debit, credit,
                    command.getCurrencyCode(), null, line.getPartnerId()));
        }

        // Auto-plug the difference to Opening Balance Equity so the entry always balances.
        BigDecimal plug = totalDebit.subtract(totalCredit);
        if (plug.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal obeDebit = plug.compareTo(BigDecimal.ZERO) < 0 ? plug.negate() : BigDecimal.ZERO;
            BigDecimal obeCredit = plug.compareTo(BigDecimal.ZERO) > 0 ? plug : BigDecimal.ZERO;
            items.add(new JournalItemCommand(obeAccount.getId().getId(), LINE_LABEL, obeDebit, obeCredit,
                    command.getCurrencyCode(), null, null));
        }

        if (items.size() < 2) {
            throw new AccountingDomainException(
                    "Opening balances need at least two non-zero lines (or one line plus the equity plug).");
        }

        CreateJournalEntryCommand createCommand = new CreateJournalEntryCommand(
                command.getCompanyId(), openingJournalId, null, command.getDate(),
                command.getCurrencyCode(), items);
        CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(createCommand);
        journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());

        return new OpeningBalanceResponse(created.getJournalEntryId(), plug,
                "Opening balances posted successfully.");
    }

    private void handleExistingOpeningEntries(CompanyId companyId, UUID openingJournalId, boolean replace) {
        List<JournalEntry> existing = journalEntryRepository.findByCompanyId(companyId).stream()
                .filter(e -> e.getJournalId() != null && openingJournalId.equals(e.getJournalId().getId()))
                .filter(e -> e.getStatus() == JournalEntryStatus.POSTED)
                .toList();
        if (existing.isEmpty()) {
            return;
        }
        if (!replace) {
            throw new AccountingDomainException(
                    "Opening balances already exist for this company. Enable replace to overwrite them.");
        }
        for (JournalEntry entry : existing) {
            journalEntryApplicationService.reverseJournalEntry(
                    new ReverseJournalEntryCommand(entry.getId().getId(), "Opening balances replaced"));
        }
    }

    private boolean isTradeAccount(AccountType type) {
        return type == AccountType.RECEIVABLE || type == AccountType.PAYABLE;
    }
}
