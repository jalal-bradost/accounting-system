package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.mapper.AccountingDataMapper;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.accounting.service.domain.ports.output.contacts.PartnerLookupPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalEntryRepository;
import com.bradox.erp.accounting.service.domain.ports.output.sequence.SequenceGeneratorPort;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.core.ValueObject.JournalId;
import com.bradox.erp.domain.core.ValueObject.PartnerRef;
import com.bradox.erp.domain.core.entity.JournalEntry;
import com.bradox.erp.domain.core.entity.JournalItem;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
class CreateJournalEntryCommandHandler {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountingDataMapper mapper;
    private final SequenceGeneratorPort sequenceGeneratorPort;
    private final CurrencyConversionPort currencyConversionPort;
    private final ObjectProvider<PartnerLookupPort> partnerLookupPortProvider;

    CreateJournalEntryCommandHandler(JournalEntryRepository journalEntryRepository,
                                    AccountingDataMapper mapper,
                                    SequenceGeneratorPort sequenceGeneratorPort,
                                    CurrencyConversionPort currencyConversionPort,
                                    ObjectProvider<PartnerLookupPort> partnerLookupPortProvider) {
        this.journalEntryRepository = journalEntryRepository;
        this.mapper = mapper;
        this.sequenceGeneratorPort = sequenceGeneratorPort;
        this.currencyConversionPort = currencyConversionPort;
        this.partnerLookupPortProvider = partnerLookupPortProvider;
    }

    @Transactional
    CreateJournalEntryResponse createJournalEntry(CreateJournalEntryCommand command) {
        CreateJournalEntryCommand normalized = withResolvedCurrency(command);
        UUID journalEntryId = UUID.randomUUID();
        String sequenceNumber = sequenceGeneratorPort.getNextSequenceNumber(
                new CompanyId(normalized.getCompanyId()),
                new JournalId(normalized.getJournalId()),
                normalized.getDate().toLocalDate());
        Function<UUID, PartnerRef> partnerResolver = partnerResolver(new CompanyId(normalized.getCompanyId()));
        List<JournalItem> items = mapper.journalItemCommandsToDomain(normalized.getItems(), partnerResolver);
        JournalEntry entry = mapper.createJournalEntryCommandToJournalEntry(
                normalized, journalEntryId, items, sequenceNumber, partnerResolver);
        JournalEntry saved = journalEntryRepository.save(entry);
        return mapper.journalEntryToCreateResponse(saved, "Journal entry created successfully.");
    }

    /**
     * When the caller omits currency (inventory valuation, etc.), use the company base currency
     * instead of inventing USD. Blank line currencies inherit the entry currency.
     */
    private CreateJournalEntryCommand withResolvedCurrency(CreateJournalEntryCommand command) {
        String currency = command.getCurrencyCode();
        if (currency == null || currency.isBlank()) {
            currency = currencyConversionPort.baseCurrencyCode(command.getCompanyId());
        } else {
            currency = currency.trim().toUpperCase();
        }
        List<JournalItemCommand> items = new ArrayList<>(command.getItems().size());
        for (JournalItemCommand item : command.getItems()) {
            String lineCurrency = item.getCurrencyCode();
            if (lineCurrency == null || lineCurrency.isBlank()) {
                lineCurrency = currency;
            } else {
                lineCurrency = lineCurrency.trim().toUpperCase();
            }
            items.add(new JournalItemCommand(
                    item.getAccountId(),
                    item.getLabel(),
                    item.getDebit(),
                    item.getCredit(),
                    lineCurrency,
                    item.getAmountCurrency(),
                    item.getPartnerId()));
        }
        return new CreateJournalEntryCommand(
                command.getCompanyId(),
                command.getJournalId(),
                command.getSequenceNumber(),
                JournalEntryTiming.ensureTimed(command.getDate()),
                currency,
                command.getPartnerId(),
                items);
    }

    private Function<UUID, PartnerRef> partnerResolver(CompanyId companyId) {
        PartnerLookupPort port = partnerLookupPortProvider.getIfAvailable();
        if (port == null) {
            return id -> new PartnerRef(id, null);
        }
        return id -> port.findByCompanyAndId(companyId, id).orElse(new PartnerRef(id, null));
    }
}
