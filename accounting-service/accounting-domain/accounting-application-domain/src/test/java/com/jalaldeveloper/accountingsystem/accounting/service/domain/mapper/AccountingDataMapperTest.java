package com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateAccountCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Account;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingDataMapperTest {

    private final AccountingDataMapper mapper = new AccountingDataMapper();

    @Test
    void journalItemCommandsToDomain_appliesScalingToDebitCredit() {
        List<JournalItemCommand> commands = List.of(
                new JournalItemCommand(UUID.randomUUID(), "line1", new BigDecimal("10.123456"), BigDecimal.ZERO, "USD", null),
                new JournalItemCommand(UUID.randomUUID(), "line2", BigDecimal.ZERO, new BigDecimal("10.123456"), "USD", null));
        List<JournalItem> items = mapper.journalItemCommandsToDomain(commands);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getDebit().scale()).isEqualTo(4);
        assertThat(items.get(0).getDebit()).isEqualByComparingTo(new BigDecimal("10.1235"));
        assertThat(items.get(0).getCredit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(items.get(1).getCredit().scale()).isEqualTo(4);
    }

    @Test
    void journalItemCommandsToDomain_returnsEmptyWhenNull() {
        assertThat(mapper.journalItemCommandsToDomain(null)).isEmpty();
    }

    @Test
    void createJournalEntryCommandToJournalEntry_usesSequenceOverrideWhenProvided() {
        UUID companyId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        CreateJournalEntryCommand cmd = new CreateJournalEntryCommand(
                companyId, journalId, "CMD-SEQ", LocalDate.of(2025, 1, 15), "USD",
                List.of(
                        new JournalItemCommand(UUID.randomUUID(), "d", BigDecimal.TEN, BigDecimal.ZERO, null, null),
                        new JournalItemCommand(UUID.randomUUID(), "c", BigDecimal.ZERO, BigDecimal.TEN, null, null)));
        List<JournalItem> items = mapper.journalItemCommandsToDomain(cmd.getItems());
        JournalEntry entry = mapper.createJournalEntryCommandToJournalEntry(cmd, entryId, items, "OVERRIDE-001");
        assertThat(entry.getSequenceNumber()).isEqualTo("OVERRIDE-001");
        assertThat(entry.getDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.DRAFT);
    }

    @Test
    void createJournalEntryCommandToJournalEntry_usesCommandSequenceWhenNoOverride() {
        UUID companyId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        CreateJournalEntryCommand cmd = new CreateJournalEntryCommand(
                companyId, journalId, "CMD-SEQ", LocalDate.now(), "USD",
                List.of(
                        new JournalItemCommand(UUID.randomUUID(), "d", BigDecimal.ONE, BigDecimal.ZERO, null, null),
                        new JournalItemCommand(UUID.randomUUID(), "c", BigDecimal.ZERO, BigDecimal.ONE, null, null)));
        List<JournalItem> items = mapper.journalItemCommandsToDomain(cmd.getItems());
        JournalEntry entry = mapper.createJournalEntryCommandToJournalEntry(cmd, UUID.randomUUID(), items);
        assertThat(entry.getSequenceNumber()).isEqualTo("CMD-SEQ");
    }

    @Test
    void accountToAccountResponse_mapsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Account account = Account.builder()
                .id(new AccountId(id))
                .companyId(new CompanyId(companyId))
                .code("1000")
                .name("Cash")
                .accountType(AccountType.BANK_AND_CASH)
                .active(true)
                .build();
        var response = mapper.accountToAccountResponse(account);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getCode()).isEqualTo("1000");
        assertThat(response.getName()).isEqualTo("Cash");
    }

    @Test
    void accountToAccountResponse_returnsNullForNull() {
        assertThat(mapper.accountToAccountResponse(null)).isNull();
    }

    @Test
    void journalToJournalResponse_mapsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Journal journal = Journal.builder()
                .id(new JournalId(id))
                .companyId(new CompanyId(companyId))
                .code("BANK")
                .name("Bank Journal")
                .journalType(JournalType.BANK)
                .build();
        var response = mapper.journalToJournalResponse(journal);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getCode()).isEqualTo("BANK");
    }

    @Test
    void journalToJournalResponse_returnsNullForNull() {
        assertThat(mapper.journalToJournalResponse(null)).isNull();
    }

    @Test
    void journalEntryToJournalEntryResponse_returnsNullForNull() {
        assertThat(mapper.journalEntryToJournalEntryResponse(null)).isNull();
    }

    @Test
    void createAccountCommandToAccount_mapsCorrectly() {
        UUID companyId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        CreateAccountCommand cmd = new CreateAccountCommand(companyId, "2000", "Receivables", AccountType.RECEIVABLE, true);
        Account account = mapper.createAccountCommandToAccount(cmd, accountId);
        assertThat(account.getId().getId()).isEqualTo(accountId);
        assertThat(account.getCompanyId().getId()).isEqualTo(companyId);
        assertThat(account.getCode()).isEqualTo("2000");
        assertThat(account.getName()).isEqualTo("Receivables");
    }
}
