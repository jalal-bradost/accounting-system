package com.bradox.erp.domain.core;

import com.bradox.erp.domain.core.ValueObject.JournalEntryId;
import com.bradox.erp.domain.core.ValueObject.JournalEntryStatus;
import com.bradox.erp.domain.core.ValueObject.JournalItemId;
import com.bradox.erp.domain.core.entity.JournalEntry;
import com.bradox.erp.domain.core.entity.JournalItem;
import com.bradox.erp.domain.core.exception.AccountingDomainException;

import java.time.LocalDateTime;
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
    public JournalEntry createReversalEntry(JournalEntry originalEntry, String reason, String reversalSequenceNumber) {
        if (originalEntry.getStatus() != JournalEntryStatus.POSTED) {
            throw new AccountingDomainException(
                    "error.accounting.cannotReverseNonPosted", null, "Cannot reverse a non-posted entry.");
        }
        if (reversalSequenceNumber == null || reversalSequenceNumber.isBlank()) {
            throw new AccountingDomainException(
                    "error.accounting.reversalSequenceRequired",
                    null,
                    "Reversal entry must have a sequence number.");
        }

        List<JournalItem> reversedItems = originalEntry.getItems().stream()
                .map(item -> JournalItem.builder()
                        .id(new JournalItemId(UUID.randomUUID()))
                        .accountId(item.getAccountId())
                        .label("Reversal of " + originalEntry.getSequenceNumber() + ": " + reason)
                        .debit(item.getCredit())
                        .credit(item.getDebit())
                        .amountCurrency(item.getAmountCurrency())
                        .currency(item.getCurrency())
                        .build())
                .toList();

        return JournalEntry.builder()
                .id(new JournalEntryId(UUID.randomUUID()))
                .companyId(originalEntry.getCompanyId())
                .journalId(originalEntry.getJournalId())
                .sequenceNumber(reversalSequenceNumber)
                .date(LocalDateTime.now())
                .currency(originalEntry.getCurrency())
                .items(reversedItems)
                .reversalOfEntryId(originalEntry.getId())
                .status(JournalEntryStatus.DRAFT)
                .build();
    }
}
