package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.mapper.AccountingDataMapper;
import com.bradox.erp.accounting.service.domain.ports.output.JournalEntryOwnershipPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalEntryManualReverseTest {

    @Mock
    private CreateJournalEntryCommandHandler createHandler;
    @Mock
    private PostJournalEntryCommandHandler postHandler;
    @Mock
    private ReverseJournalEntryCommandHandler reverseHandler;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalEntryOwnershipPort ownershipPort;
    @Mock
    private AccountingDataMapper mapper;

    private JournalEntryApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JournalEntryApplicationServiceImpl(
                createHandler, postHandler, reverseHandler, journalEntryRepository, ownershipPort, mapper);
    }

    @Test
    void refusesEntryOwnedByDocument() {
        UUID id = UUID.randomUUID();
        when(ownershipPort.findOwner(id)).thenReturn(Optional.of("customer invoice INV/2026/00048"));

        assertThatThrownBy(() -> service.reverseManualJournalEntry(new ReverseJournalEntryCommand(id, "test")))
                .isInstanceOf(AccountingDomainException.class)
                .hasMessageContaining("INV/2026/00048");
        verify(reverseHandler, never()).reverseJournalEntry(any());
    }

    @Test
    void reversesUnownedEntry() {
        UUID id = UUID.randomUUID();
        ReverseJournalEntryCommand command = new ReverseJournalEntryCommand(id, "test");
        ReverseJournalEntryResponse response = mock(ReverseJournalEntryResponse.class);
        when(ownershipPort.findOwner(id)).thenReturn(Optional.empty());
        when(reverseHandler.reverseJournalEntry(command)).thenReturn(response);

        assertThat(service.reverseManualJournalEntry(command)).isSameAs(response);
    }

    @Test
    void internalReverseSkipsOwnershipCheck() {
        ReverseJournalEntryCommand command = new ReverseJournalEntryCommand(UUID.randomUUID(), "payment");

        service.reverseJournalEntry(command);

        verify(reverseHandler).reverseJournalEntry(command);
        verify(ownershipPort, never()).findOwner(any());
    }
}
