package com.bradox.erp.integration;

import com.bradox.erp.accounting.service.domain.JournalEntryTiming;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.dataaccess.entity.JournalEntity;
import com.bradox.erp.dataaccess.repository.JournalJpaRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.service.domain.ports.output.accounting.JournalEntryPostingPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Infrastructure-side adapter that fulfils the inventory module's {@link JournalEntryPostingPort}
 * by delegating to the accounting module's {@link JournalEntryApplicationService}.
 *
 * <p>Inventory entries are routed to the journal whose {@code code} is {@code INV}; the
 * caller is expected to have provisioned this journal (otherwise the call fails with a
 * descriptive error). Entries are created and immediately posted so the resulting JE id
 * is available for valuation-layer linkage. Currency is the company base currency.
 */
@Component
public class JournalEntryPostingAdapter implements JournalEntryPostingPort {

    private static final String INVENTORY_JOURNAL_CODE = "INV";

    private final JournalEntryApplicationService journalService;
    private final JournalJpaRepository journalRepository;
    private final CurrencyConversionPort currencyConversionPort;

    public JournalEntryPostingAdapter(JournalEntryApplicationService journalService,
                                      JournalJpaRepository journalRepository,
                                      CurrencyConversionPort currencyConversionPort) {
        this.journalService = journalService;
        this.journalRepository = journalRepository;
        this.currencyConversionPort = currencyConversionPort;
    }

    @Override
    public UUID postValuationEntry(CompanyId companyId, LocalDate entryDate, String reference,
                                    UUID partnerId, List<JournalLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        JournalEntity journal = journalRepository
                .findByCompanyIdAndCode(companyId.getId(), INVENTORY_JOURNAL_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Inventory journal '" + INVENTORY_JOURNAL_CODE + "' not configured for company "
                                + companyId.getId() + " (create a journal with code INV)"));

        String currencyCode = currencyConversionPort.baseCurrencyCode(companyId.getId());
        List<JournalItemCommand> items = new ArrayList<>(lines.size());
        for (JournalLine line : lines) {
            items.add(new JournalItemCommand(
                    line.accountId(), line.label(),
                    line.debit() != null ? line.debit().getAmount() : null,
                    line.credit() != null ? line.credit().getAmount() : null,
                    currencyCode, null, partnerId));
        }
        CreateJournalEntryCommand command = new CreateJournalEntryCommand(
                companyId.getId(), journal.getId(), reference, JournalEntryTiming.ofBusinessDate(entryDate),
                currencyCode, partnerId, items);
        CreateJournalEntryResponse created = journalService.createJournalEntry(command);
        journalService.postJournalEntry(created.getJournalEntryId());
        return created.getJournalEntryId();
    }
}
