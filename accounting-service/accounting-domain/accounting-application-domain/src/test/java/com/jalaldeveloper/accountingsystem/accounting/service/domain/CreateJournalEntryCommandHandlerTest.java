package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateJournalEntryCommandHandlerTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private AccountingDataMapper mapper;
    @Mock
    private SequenceGeneratorPort sequenceGeneratorPort;

    @InjectMocks
    private CreateJournalEntryCommandHandler handler;

    @Test
    void createJournalEntry_callsSequenceGeneratorAndSavesEntry() {
        UUID companyId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        List<JournalItemCommand> items = List.of(
                new JournalItemCommand(UUID.randomUUID(), "d", BigDecimal.TEN, BigDecimal.ZERO, null, null),
                new JournalItemCommand(UUID.randomUUID(), "c", BigDecimal.ZERO, BigDecimal.TEN, null, null));
        CreateJournalEntryCommand command = new CreateJournalEntryCommand(
                companyId, journalId, null, LocalDate.of(2025, 3, 1), "USD", items);

        when(sequenceGeneratorPort.getNextSequenceNumber(any(CompanyId.class), any(JournalId.class), any(LocalDate.class)))
                .thenReturn("JOU-2025-00001");
        JournalEntry entryToSave = JournalEntry.builder()
                .id(new JournalEntryId(UUID.randomUUID()))
                .companyId(new CompanyId(companyId))
                .journalId(new JournalId(journalId))
                .sequenceNumber("JOU-2025-00001")
                .date(LocalDate.of(2025, 3, 1))
                .items(List.of())
                .status(JournalEntryStatus.DRAFT)
                .build();
        when(mapper.journalItemCommandsToDomain(any())).thenReturn(List.of());
        when(mapper.createJournalEntryCommandToJournalEntry(any(), any(), any(), any())).thenReturn(entryToSave);
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.journalEntryToCreateResponse(any(JournalEntry.class), any())).thenReturn(new CreateJournalEntryResponse(entryToSave.getId().getId(), "Journal entry created successfully."));

        CreateJournalEntryResponse response = handler.createJournalEntry(command);

        assertThat(response).isNotNull();
        verify(sequenceGeneratorPort).getNextSequenceNumber(
                new CompanyId(companyId), new JournalId(journalId), LocalDate.of(2025, 3, 1));
        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry captured = entryCaptor.getValue();
        assertThat(captured.getSequenceNumber()).isEqualTo("JOU-2025-00001");
        assertThat(captured.getStatus()).isEqualTo(JournalEntryStatus.DRAFT);
    }
}
