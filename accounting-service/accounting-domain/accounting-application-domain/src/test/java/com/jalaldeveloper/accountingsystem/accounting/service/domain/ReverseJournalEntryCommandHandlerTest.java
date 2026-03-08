package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.jalaldeveloper.accountingsystem.domain.core.AccountingDomainService;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalItemId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReverseJournalEntryCommandHandlerTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private AccountingDomainService accountingDomainService;
    @Mock
    private AccountingDataMapper mapper;
    @Mock
    private SequenceGeneratorPort sequenceGeneratorPort;

    private ReverseJournalEntryCommandHandler handler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        handler = new ReverseJournalEntryCommandHandler(
                journalEntryRepository, accountingDomainService, mapper, sequenceGeneratorPort);
    }

    @Test
    void reverseJournalEntry_throwsWhenEntryNotFound() {
        UUID entryId = UUID.randomUUID();
        when(journalEntryRepository.findById(new JournalEntryId(entryId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.reverseJournalEntry(new ReverseJournalEntryCommand(entryId, "reason")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void reverseJournalEntry_createsReversalAndPosts() {
        UUID originalId = UUID.randomUUID();
        UUID reversalId = UUID.randomUUID();
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        JournalId journalId = new JournalId(UUID.randomUUID());
        JournalEntry posted = postedEntry(originalId, companyId, journalId);
        JournalEntry reversalDraft = JournalEntry.builder()
                .id(new JournalEntryId(reversalId))
                .companyId(companyId)
                .journalId(journalId)
                .sequenceNumber("REV-001")
                .date(LocalDate.now())
                .currency(Currency.USD())
                .items(List.of())
                .status(JournalEntryStatus.DRAFT)
                .build();
        JournalEntry reversalPosted = JournalEntry.builder()
                .id(new JournalEntryId(reversalId))
                .companyId(companyId)
                .journalId(journalId)
                .sequenceNumber("REV-001")
                .date(LocalDate.now())
                .currency(Currency.USD())
                .items(List.of())
                .status(JournalEntryStatus.POSTED)
                .build();

        when(journalEntryRepository.findById(new JournalEntryId(originalId))).thenReturn(Optional.of(posted));
        when(sequenceGeneratorPort.getNextSequenceNumber(any(CompanyId.class), any(JournalId.class), any(LocalDate.class))).thenReturn("REV-001");
        when(accountingDomainService.createReversalEntry(any(JournalEntry.class), any(), any())).thenReturn(reversalDraft);
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        when(mapper.toReverseResponse(any(JournalEntry.class), any(JournalEntry.class), any()))
                .thenReturn(new ReverseJournalEntryResponse(originalId, reversalId, "Reversal entry created and posted successfully."));

        ReverseJournalEntryResponse response = handler.reverseJournalEntry(new ReverseJournalEntryCommand(originalId, "reason"));

        assertThat(response).isNotNull();
        verify(accountingDomainService).createReversalEntry(any(JournalEntry.class), eq("reason"), eq("REV-001"));
        verify(accountingDomainService).postJournalEntry(any(JournalEntry.class));
        verify(journalEntryRepository, times(2)).save(any(JournalEntry.class));
    }

    private static JournalEntry postedEntry(UUID entryId, CompanyId companyId, JournalId journalId) {
        List<JournalItem> items = List.of(
                JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.TEN).credit(BigDecimal.ZERO).build(),
                JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.ZERO).credit(BigDecimal.TEN).build());
        JournalEntry entry = JournalEntry.builder()
                .id(new JournalEntryId(entryId))
                .companyId(companyId)
                .journalId(journalId)
                .sequenceNumber("JOU-001")
                .date(LocalDate.now())
                .currency(Currency.USD())
                .items(items)
                .status(JournalEntryStatus.DRAFT)
                .build();
        entry.post();
        return entry;
    }
}
