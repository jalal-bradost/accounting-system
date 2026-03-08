package com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.*;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.*;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Account;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.AccountResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalEntryResponse;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import com.jalaldeveloper.accountingsystem.domain.valueobject.MonetaryScale;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccountingDataMapper {

    public Account createAccountCommandToAccount(CreateAccountCommand cmd, UUID accountId) {
        return Account.builder()
                .id(new AccountId(accountId))
                .companyId(new CompanyId(cmd.getCompanyId()))
                .code(cmd.getCode())
                .name(cmd.getName())
                .accountType(cmd.getAccountType())
                .active(cmd.isActive())
                .build();
    }

    public CreateAccountResponse accountToCreateAccountResponse(Account account, String message) {
        return new CreateAccountResponse(account.getId().getId(), message);
    }

    public Journal createJournalCommandToJournal(CreateJournalCommand cmd, UUID journalId) {
        return Journal.builder()
                .id(new JournalId(journalId))
                .companyId(new CompanyId(cmd.getCompanyId()))
                .code(cmd.getCode())
                .name(cmd.getName())
                .journalType(cmd.getJournalType())
                .build();
    }

    public CreateJournalResponse journalToCreateJournalResponse(Journal journal, String message) {
        return new CreateJournalResponse(journal.getId().getId(), message);
    }

    public JournalEntry createJournalEntryCommandToJournalEntry(CreateJournalEntryCommand cmd,
                                                                UUID journalEntryId,
                                                                List<JournalItem> items) {
        return createJournalEntryCommandToJournalEntry(cmd, journalEntryId, items, null);
    }

    /** When sequenceNumberOverride is non-null, it is used instead of command.getSequenceNumber(). */
    public JournalEntry createJournalEntryCommandToJournalEntry(CreateJournalEntryCommand cmd,
                                                                UUID journalEntryId,
                                                                List<JournalItem> items,
                                                                String sequenceNumberOverride) {
        Currency currency = toCurrency(cmd.getCurrencyCode());
        String seq = sequenceNumberOverride != null && !sequenceNumberOverride.isBlank()
                ? sequenceNumberOverride
                : (cmd.getSequenceNumber() != null && !cmd.getSequenceNumber().isBlank()
                        ? cmd.getSequenceNumber()
                        : "TMP-" + System.currentTimeMillis());
        return JournalEntry.builder()
                .id(new JournalEntryId(journalEntryId))
                .companyId(new CompanyId(cmd.getCompanyId()))
                .journalId(new JournalId(cmd.getJournalId()))
                .sequenceNumber(seq)
                .date(cmd.getDate())
                .currency(currency)
                .items(items)
                .status(JournalEntryStatus.DRAFT)
                .build();
    }

    public List<JournalItem> journalItemCommandsToDomain(List<JournalItemCommand> commands) {
        if (commands == null) return List.of();
        return commands.stream()
                .map(c -> {
                    Money amountCurrency = c.getAmountCurrency() != null
                            ? new Money(c.getAmountCurrency())
                            : Money.ZERO;
                    Currency curr = toCurrency(c.getCurrencyCode());
                    return JournalItem.builder()
                            .id(new JournalItemId(UUID.randomUUID()))
                            .accountId(new AccountId(c.getAccountId()))
                            .label(c.getLabel())
                            .debit(MonetaryScale.scale(c.getDebit()))
                            .credit(MonetaryScale.scale(c.getCredit()))
                            .amountCurrency(amountCurrency)
                            .currency(curr)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public CreateJournalEntryResponse journalEntryToCreateResponse(JournalEntry entry, String message) {
        return new CreateJournalEntryResponse(entry.getId().getId(), message);
    }

    public ReverseJournalEntryResponse toReverseResponse(JournalEntry original, JournalEntry reversal, String message) {
        return new ReverseJournalEntryResponse(original.getId().getId(), reversal.getId().getId(), message);
    }

    public AccountResponse accountToAccountResponse(Account account) {
        if (account == null) return null;
        return new AccountResponse(
                account.getId().getId(),
                account.getCompanyId().getId(),
                account.getCode(),
                account.getName(),
                account.getAccountType(),
                account.isActive());
    }

    public JournalResponse journalToJournalResponse(Journal journal) {
        if (journal == null) return null;
        return new JournalResponse(
                journal.getId().getId(),
                journal.getCompanyId().getId(),
                journal.getCode(),
                journal.getName(),
                journal.getJournalType());
    }

    public JournalEntryResponse journalEntryToJournalEntryResponse(JournalEntry entry) {
        if (entry == null) return null;
        List<JournalEntryResponse.JournalItemResponse> itemResponses = entry.getItems() == null ? List.of() :
                entry.getItems().stream()
                        .map(i -> new JournalEntryResponse.JournalItemResponse(
                                i.getId().getId(),
                                i.getAccountId().getId(),
                                i.getLabel(),
                                i.getDebit(),
                                i.getCredit(),
                                i.getCurrency() != null ? i.getCurrency().code() : null,
                                i.getAmountCurrency() != null ? i.getAmountCurrency().getAmount() : null,
                                i.getReconciliationId()))
                        .collect(Collectors.toList());
        return new JournalEntryResponse(
                entry.getId().getId(),
                entry.getCompanyId().getId(),
                entry.getJournalId().getId(),
                entry.getSequenceNumber(),
                entry.getDate(),
                entry.getCurrency() != null ? entry.getCurrency().code() : null,
                entry.getStatus(),
                entry.getReversalOfEntryId() != null ? entry.getReversalOfEntryId().getId() : null,
                itemResponses,
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                entry.getPostedAt(),
                entry.getPostedBy());
    }

    private static Currency toCurrency(String code) {
        if (code == null || code.isBlank()) return Currency.USD();
        return new Currency(code, "", 2);
    }
}
