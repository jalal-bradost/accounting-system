package com.jalaldeveloper.accountingsystem.domain.core;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalItemId;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountingDomainServiceImplTest {

    private AccountingDomainService domainService;
    private CompanyId companyId;
    private JournalId journalId;
    private JournalEntryId entryId;

    @BeforeEach
    void setUp() {
        domainService = new AccountingDomainServiceImpl();
        companyId = new CompanyId(UUID.randomUUID());
        journalId = new JournalId(UUID.randomUUID());
        entryId = new JournalEntryId(UUID.randomUUID());
    }

    private JournalEntry postedEntryWithTwoLines(BigDecimal amount) {
        JournalItem debit = JournalItem.builder()
                .id(new JournalItemId(UUID.randomUUID()))
                .accountId(new AccountId(UUID.randomUUID()))
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .build();
        JournalItem credit = JournalItem.builder()
                .id(new JournalItemId(UUID.randomUUID()))
                .accountId(new AccountId(UUID.randomUUID()))
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .build();
        JournalEntry entry = JournalEntry.builder()
                .id(entryId)
                .companyId(companyId)
                .journalId(journalId)
                .sequenceNumber("JOU-001")
                .date(LocalDate.now())
                .currency(Currency.USD())
                .items(List.of(debit, credit))
                .status(JournalEntryStatus.DRAFT)
                .build();
        entry.post();
        return entry;
    }

    @Test
    void postJournalEntry_validatesAndPosts() {
        JournalEntry entry = JournalEntry.builder()
                .id(entryId)
                .companyId(companyId)
                .journalId(journalId)
                .sequenceNumber("JOU-001")
                .date(LocalDate.now())
                .currency(Currency.USD())
                .items(List.of(
                        JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.TEN).credit(BigDecimal.ZERO).build(),
                        JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.ZERO).credit(BigDecimal.TEN).build()))
                .status(JournalEntryStatus.DRAFT)
                .build();
        domainService.postJournalEntry(entry);
        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
    }

    @Test
    void createReversalEntry_throwsWhenNotPosted() {
        JournalEntry draft = JournalEntry.builder()
                .id(entryId)
                .companyId(companyId)
                .journalId(journalId)
                .sequenceNumber("JOU-001")
                .date(LocalDate.now())
                .currency(Currency.USD())
                .items(List.of(
                        JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.ONE).credit(BigDecimal.ZERO).build(),
                        JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.ZERO).credit(BigDecimal.ONE).build()))
                .status(JournalEntryStatus.DRAFT)
                .build();
        assertThatThrownBy(() -> domainService.createReversalEntry(draft, "reason", "REV-001"))
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("Cannot reverse a non-posted");
    }

    @Test
    void createReversalEntry_throwsWhenBlankSequenceNumber() {
        JournalEntry posted = postedEntryWithTwoLines(BigDecimal.TEN);
        assertThatThrownBy(() -> domainService.createReversalEntry(posted, "reason", " "))
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("sequence number");
        assertThatThrownBy(() -> domainService.createReversalEntry(posted, "reason", null))
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("sequence number");
    }

    @Test
    void createReversalEntry_returnsReversalWithSwappedDebitCreditAndLink() {
        JournalEntry posted = postedEntryWithTwoLines(new BigDecimal("100"));
        String revSeq = "REV-001";
        JournalEntry reversal = domainService.createReversalEntry(posted, "correction", revSeq);

        assertThat(reversal.getId()).isNotEqualTo(posted.getId());
        assertThat(reversal.getReversalOfEntryId()).isEqualTo(posted.getId());
        assertThat(reversal.getStatus()).isEqualTo(JournalEntryStatus.DRAFT);
        assertThat(reversal.getSequenceNumber()).isEqualTo(revSeq);
        assertThat(reversal.getCompanyId()).isEqualTo(posted.getCompanyId());
        assertThat(reversal.getJournalId()).isEqualTo(posted.getJournalId());
        assertThat(reversal.getItems()).hasSize(2);
        reversal.getItems().forEach(item -> {
            assertThat(item.getLabel()).contains("Reversal of").contains("correction");
        });
        BigDecimal firstDebit = posted.getItems().get(0).getDebit();
        BigDecimal firstCredit = posted.getItems().get(0).getCredit();
        assertThat(reversal.getItems().get(0).getDebit()).isEqualByComparingTo(firstCredit);
        assertThat(reversal.getItems().get(0).getCredit()).isEqualByComparingTo(firstDebit);
    }
}
