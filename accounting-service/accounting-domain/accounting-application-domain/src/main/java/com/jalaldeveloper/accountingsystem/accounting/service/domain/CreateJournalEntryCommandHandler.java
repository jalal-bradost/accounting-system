package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.mapper.AccountingDataMapper;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.contacts.PartnerLookupPort;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.PartnerRef;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalEntry;
import com.jalaldeveloper.accountingsystem.domain.core.entity.JournalItem;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
class CreateJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDataMapper mapper;
    private final SequenceGeneratorPort sequenceGeneratorPort;
    private final ObjectProvider<PartnerLookupPort> partnerLookupPortProvider;

    CreateJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                    AccountingDataMapper mapper,
                                    SequenceGeneratorPort sequenceGeneratorPort,
                                    ObjectProvider<PartnerLookupPort> partnerLookupPortProvider) {
        this.journalEntryRepository = journalEntryRepository;
        this.mapper = mapper;
        this.sequenceGeneratorPort = sequenceGeneratorPort;
        this.partnerLookupPortProvider = partnerLookupPortProvider;
    }

    @Transactional
    CreateJournalEntryResponse createJournalEntry(CreateJournalEntryCommand command) {
        UUID journalEntryId = UUID.randomUUID();
        String sequenceNumber = sequenceGeneratorPort.getNextSequenceNumber(
                new CompanyId(command.getCompanyId()),
                new JournalId(command.getJournalId()),
                command.getDate());
        Function<UUID, PartnerRef> partnerResolver = partnerResolver(new CompanyId(command.getCompanyId()));
        List<JournalItem> items = mapper.journalItemCommandsToDomain(command.getItems(), partnerResolver);
        JournalEntry entry = mapper.createJournalEntryCommandToJournalEntry(
                command, journalEntryId, items, sequenceNumber, partnerResolver);
        JournalEntry saved = journalEntryRepository.save(entry);
        return mapper.journalEntryToCreateResponse(saved, "Journal entry created successfully.");
    }

    private Function<UUID, PartnerRef> partnerResolver(CompanyId companyId) {
        PartnerLookupPort port = partnerLookupPortProvider.getIfAvailable();
        if (port == null) {
            return id -> new PartnerRef(id, null);
        }
        return id -> port.findByCompanyAndId(companyId, id).orElse(new PartnerRef(id, null));
    }
}
