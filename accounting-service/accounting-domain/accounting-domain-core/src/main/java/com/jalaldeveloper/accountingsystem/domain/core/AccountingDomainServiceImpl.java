package com.jalaldeveloper.accountingsystem.domain.core;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalItemId;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class AccountingDomainServiceImpl implements AccountingDomainService {

    @Override
    public void postJournalEntry(JournalEntry journalEntry) {
        // 1. Perform domain-level validation (Debits == Credits)
        journalEntry.validate();

        // 2. Update the state
        journalEntry.post();

    }

    @Override
    public JournalEntry createReversalEntry(JournalEntry originalEntry, String reason) {
        if (originalEntry.getStatus() != JournalEntryStatus.POSTED) {
            throw new AccountingDomainException("Cannot reverse a non-posted entry.");
        }

        // Map original items to reversed items
        List<JournalItem> reversedItems = originalEntry.getItems().stream()
                .map(item -> JournalItem.builder()
                        .id(new JournalItemId(UUID.randomUUID()))
                        .accountId(item.getAccountId())
                        .label("Reversal of " + originalEntry.getSequenceNumber() + ": " + reason)
                        .debit(item.getCredit())  // Swap Credit to Debit
                        .credit(item.getDebit()) // Swap Debit to Credit
                        .amountCurrency(item.getAmountCurrency())
                        .currency(item.getCurrency())
                        .build())
                .toList();

        // Create the new JournalEntry using its Builder
        return JournalEntry.builder()
                .id(new JournalEntryId(UUID.randomUUID()))
                .companyId(originalEntry.getCompanyId())
                .journalId(originalEntry.getJournalId())
                .date(LocalDate.now())
                .currency(originalEntry.getCurrency())
                .items(reversedItems)
                .status(JournalEntryStatus.DRAFT)
                .build();
    }
}
