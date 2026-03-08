package com.jalaldeveloper.accountingsystem.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalItemId;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

    private static final CompanyId COMPANY_ID = new CompanyId(UUID.randomUUID());
    private static final JournalId JOURNAL_ID = new JournalId(UUID.randomUUID());
    private static final Currency CURRENCY = Currency.USD();

    private static JournalItem debitItem(BigDecimal amount) {
        return JournalItem.builder()
                .id(new JournalItemId(UUID.randomUUID()))
                .accountId(new AccountId(UUID.randomUUID()))
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .build();
    }

    private static JournalItem creditItem(BigDecimal amount) {
        return JournalItem.builder()
                .id(new JournalItemId(UUID.randomUUID()))
                .accountId(new AccountId(UUID.randomUUID()))
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .build();
    }

    private static JournalEntry balancedDraftEntry(List<JournalItem> items) {
        return JournalEntry.builder()
                .id(new JournalEntryId(UUID.randomUUID()))
                .companyId(COMPANY_ID)
                .journalId(JOURNAL_ID)
                .sequenceNumber("SEQ-001")
                .date(LocalDate.now())
                .currency(CURRENCY)
                .items(items)
                .status(JournalEntryStatus.DRAFT)
                .build();
    }

    @Test
    void validate_throwsWhenFewerThanTwoLines() {
        JournalEntry entry = balancedDraftEntry(List.of(debitItem(BigDecimal.TEN)));
        assertThatThrownBy(entry::validate)
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("at least two lines");
    }

    @Test
    void validate_throwsWhenLineHasBothDebitAndCredit() {
        JournalItem bad = JournalItem.builder()
                .id(new JournalItemId(UUID.randomUUID()))
                .accountId(new AccountId(UUID.randomUUID()))
                .debit(BigDecimal.ONE)
                .credit(BigDecimal.ONE)
                .build();
        JournalEntry entry = balancedDraftEntry(List.of(bad, creditItem(BigDecimal.ONE)));
        assertThatThrownBy(entry::validate)
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("either debit or credit");
    }

    @Test
    void validate_throwsWhenLineHasBothZero() {
        JournalItem zero = JournalItem.builder()
                .id(new JournalItemId(UUID.randomUUID()))
                .accountId(new AccountId(UUID.randomUUID()))
                .debit(BigDecimal.ZERO)
                .credit(BigDecimal.ZERO)
                .build();
        JournalEntry entry = balancedDraftEntry(List.of(zero, creditItem(BigDecimal.TEN)));
        assertThatThrownBy(entry::validate)
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("either debit or credit");
    }

    @Test
    void validate_throwsWhenUnbalanced() {
        JournalEntry entry = balancedDraftEntry(List.of(
                debitItem(new BigDecimal("100")),
                creditItem(new BigDecimal("99"))));
        assertThatThrownBy(entry::validate)
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("not balanced");
    }

    @Test
    void validate_passesWhenBalanced() {
        JournalEntry entry = balancedDraftEntry(List.of(
                debitItem(new BigDecimal("100")),
                creditItem(new BigDecimal("100"))));
        entry.validate();
    }

    @Test
    void post_throwsWhenNotDraft() {
        JournalEntry entry = balancedDraftEntry(List.of(
                debitItem(BigDecimal.TEN),
                creditItem(BigDecimal.TEN)));
        entry.post();
        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThatThrownBy(entry::post)
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("Only Draft");
    }

    @Test
    void post_throwsWhenEmptyItems() {
        JournalEntry entry = JournalEntry.builder()
                .id(new JournalEntryId(UUID.randomUUID()))
                .companyId(COMPANY_ID)
                .journalId(JOURNAL_ID)
                .sequenceNumber("SEQ-001")
                .date(LocalDate.now())
                .currency(CURRENCY)
                .items(List.of())
                .status(JournalEntryStatus.DRAFT)
                .build();
        assertThatThrownBy(entry::post)
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void post_setsStatusPosted() {
        JournalEntry entry = balancedDraftEntry(List.of(
                debitItem(BigDecimal.TEN),
                creditItem(BigDecimal.TEN)));
        entry.post();
        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
    }

    @Test
    void cancel_throwsWhenNotDraft() {
        JournalEntry entry = balancedDraftEntry(List.of(
                debitItem(BigDecimal.TEN),
                creditItem(BigDecimal.TEN)));
        entry.post();
        assertThatThrownBy(entry::cancel)
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("Only DRAFT");
    }

    @Test
    void cancel_setsStatusCancelled() {
        JournalEntry entry = balancedDraftEntry(List.of(
                debitItem(BigDecimal.TEN),
                creditItem(BigDecimal.TEN)));
        entry.cancel();
        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.CANCELLED);
    }
}
