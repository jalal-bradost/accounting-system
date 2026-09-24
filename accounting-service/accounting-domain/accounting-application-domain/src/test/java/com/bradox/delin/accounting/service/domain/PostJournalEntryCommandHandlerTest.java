package com.bradox.delin.accounting.service.domain;

import com.bradox.delin.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.delin.accounting.service.domain.mapper.AccountingDataMapper;
import com.bradox.delin.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.bradox.delin.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.delin.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.bradox.delin.domain.core.AccountingDomainService;
import com.bradox.delin.domain.core.ValueObject.JournalEntryId;
import com.bradox.delin.domain.core.ValueObject.JournalEntryStatus;
import com.bradox.delin.domain.core.entity.JournalEntry;
import com.bradox.delin.domain.core.entity.JournalItem;
import com.bradox.delin.domain.core.ValueObject.AccountId;
import com.bradox.delin.domain.core.ValueObject.JournalItemId;
import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.domain.core.ValueObject.JournalId;
import com.bradox.delin.domain.valueobject.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostJournalEntryCommandHandlerTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private AccountingDomainService accountingDomainService;
    @Mock
    private AccountingDataMapper mapper;
    @Mock
    private CompanyLockDatePort companyLockDatePort;
    @Mock
    private FiscalPeriodRepository fiscalPeriodRepository;

    private PostJournalEntryCommandHandler handler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        PeriodPostingGuard guard = new PeriodPostingGuard(companyLockDatePort, fiscalPeriodRepository);
        handler = new PostJournalEntryCommandHandler(
                journalEntryRepository, accountingDomainService, mapper, guard);
    }

    @Test
    void postJournalEntry_returnsAlreadyPostedMessageWhenStatusIsPosted() {
        UUID entryId = UUID.randomUUID();
        JournalEntry postedEntry = draftEntry(entryId);
        postedEntry.post();

        when(journalEntryRepository.findById(new JournalEntryId(entryId))).thenReturn(Optional.of(postedEntry));
        when(mapper.journalEntryToCreateResponse(any(JournalEntry.class), any())).thenReturn(
                new CreateJournalEntryResponse(entryId, "Journal entry already posted."));

        CreateJournalEntryResponse response = handler.postJournalEntry(entryId);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("already posted");
        verify(mapper).journalEntryToCreateResponse(postedEntry, "Journal entry already posted.");
    }

    @Test
    void postJournalEntry_throwsWhenEntryDateOnOrBeforeLockDate() {
        UUID entryId = UUID.randomUUID();
        JournalEntry draft = draftEntryWithDate(entryId, LocalDate.of(2025, 3, 1).atStartOfDay());
        when(journalEntryRepository.findById(new JournalEntryId(entryId))).thenReturn(Optional.of(draft));
        when(companyLockDatePort.getPeriodLockDate(any(CompanyId.class)))
                .thenReturn(Optional.of(LocalDate.of(2025, 3, 15)));

        assertThatThrownBy(() -> handler.postJournalEntry(entryId))
                .hasMessageContaining("on or before period lock date");
    }

    @Test
    void postJournalEntry_postsAndSavesWhenNoLockDateAndPeriodOpen() {
        UUID entryId = UUID.randomUUID();
        JournalEntry draft = draftEntry(entryId);
        when(journalEntryRepository.findById(new JournalEntryId(entryId))).thenReturn(Optional.of(draft));
        when(companyLockDatePort.getPeriodLockDate(any(CompanyId.class))).thenReturn(Optional.empty());
        when(fiscalPeriodRepository.findPeriodContaining(any(CompanyId.class), any(LocalDate.class))).thenReturn(Optional.empty());
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.journalEntryToCreateResponse(any(JournalEntry.class), any()))
                .thenReturn(new CreateJournalEntryResponse(entryId, "Journal entry posted successfully."));

        CreateJournalEntryResponse response = handler.postJournalEntry(entryId);

        assertThat(response).isNotNull();
        verify(accountingDomainService).postJournalEntry(draft);
        verify(journalEntryRepository).save(draft);
    }

    private static JournalEntry draftEntry(UUID entryId) {
        return draftEntryWithDate(entryId, LocalDateTime.now());
    }

    private static JournalEntry draftEntryWithDate(UUID entryId, LocalDateTime date) {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        JournalId journalId = new JournalId(UUID.randomUUID());
        List<JournalItem> items = List.of(
                JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.TEN).credit(BigDecimal.ZERO).build(),
                JournalItem.builder().id(new JournalItemId(UUID.randomUUID())).accountId(new AccountId(UUID.randomUUID())).debit(BigDecimal.ZERO).credit(BigDecimal.TEN).build());
        return JournalEntry.builder()
                .id(new JournalEntryId(entryId))
                .companyId(companyId)
                .journalId(journalId)
                .sequenceNumber("SEQ-1")
                .date(date)
                .currency(Currency.USD())
                .items(items)
                .status(JournalEntryStatus.DRAFT)
                .build();
    }
}
