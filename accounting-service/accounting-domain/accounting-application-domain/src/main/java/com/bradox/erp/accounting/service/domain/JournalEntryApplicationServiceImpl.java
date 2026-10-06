package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.mapper.AccountingDataMapper;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.JournalEntryOwnershipPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.erp.domain.core.ValueObject.JournalEntryId;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class JournalEntryApplicationServiceImpl implements JournalEntryApplicationService {

    private final CreateJournalEntryCommandHandler createJournalEntryCommandHandler;
    private final PostJournalEntryCommandHandler postJournalEntryCommandHandler;
    private final ReverseJournalEntryCommandHandler reverseJournalEntryCommandHandler;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryOwnershipPort journalEntryOwnershipPort;
    private final AccountingDataMapper mapper;

    JournalEntryApplicationServiceImpl(CreateJournalEntryCommandHandler createJournalEntryCommandHandler,
                                       PostJournalEntryCommandHandler postJournalEntryCommandHandler,
                                       ReverseJournalEntryCommandHandler reverseJournalEntryCommandHandler,
                                       JournalEntryRepository journalEntryRepository,
                                       JournalEntryOwnershipPort journalEntryOwnershipPort,
                                       AccountingDataMapper mapper) {
        this.createJournalEntryCommandHandler = createJournalEntryCommandHandler;
        this.postJournalEntryCommandHandler = postJournalEntryCommandHandler;
        this.reverseJournalEntryCommandHandler = reverseJournalEntryCommandHandler;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryOwnershipPort = journalEntryOwnershipPort;
        this.mapper = mapper;
    }

    @Override
    public CreateJournalEntryResponse createJournalEntry(CreateJournalEntryCommand command) {
        return createJournalEntryCommandHandler.createJournalEntry(command);
    }

    @Override
    public CreateJournalEntryResponse postJournalEntry(UUID journalEntryId) {
        return postJournalEntryCommandHandler.postJournalEntry(journalEntryId);
    }

    @Override
    public ReverseJournalEntryResponse reverseJournalEntry(ReverseJournalEntryCommand command) {
        return reverseJournalEntryCommandHandler.reverseJournalEntry(command);
    }

    @Override
    public ReverseJournalEntryResponse reverseManualJournalEntry(ReverseJournalEntryCommand command) {
        // Document entries must be undone through their document (credit note, cancel, ...) so the
        // document state, balances and linked records stay in step with the ledger.
        journalEntryOwnershipPort.findOwner(command.getJournalEntryId()).ifPresent(owner -> {
            throw new AccountingDomainException("error.accounting.journalEntryOwnedByDocument",
                    new Object[]{owner},
                    "This journal entry belongs to " + owner + "; cancel or credit that document instead");
        });
        return reverseJournalEntryCommandHandler.reverseJournalEntry(command);
    }

    @Override
    public JournalEntryResponse getJournalEntry(UUID journalEntryId) {
        return journalEntryRepository.findById(new JournalEntryId(journalEntryId))
                .map(mapper::journalEntryToJournalEntryResponse)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + journalEntryId));
    }

    @Override
    public List<JournalEntryResponse> listJournalEntriesByCompany(UUID companyId) {
        return journalEntryRepository.findByCompanyId(new CompanyId(companyId)).stream()
                .map(mapper::journalEntryToJournalEntryResponse)
                .sorted((a, b) -> {
                    int byDate = b.getDate().compareTo(a.getDate());
                    if (byDate != 0) return byDate;
                    return b.getSequenceNumber().compareTo(a.getSequenceNumber());
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<JournalEntryResponse> searchJournalEntries(UUID companyId, Pageable pageable) {
        return journalEntryRepository.searchByCompanyId(new CompanyId(companyId), pageable)
                .map(mapper::journalEntryToJournalEntryResponse);
    }
}
