package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.accounting;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Output port that lets inventory post valuation journal entries without depending on the
 * accounting module at the Maven level. Implemented in the container as an adapter over the
 * existing {@code JournalEntryApplicationService}.
 *
 * <p>Inventory always submits balanced (debit == credit) entries; the adapter is responsible
 * for picking the inventory journal (e.g. by code "INV") and translating account UUIDs.
 */
public interface JournalEntryPostingPort {

    UUID postValuationEntry(CompanyId companyId,
                            LocalDate entryDate,
                            String reference,
                            UUID partnerId,
                            List<JournalLine> lines);

    record JournalLine(UUID accountId, String label, Money debit, Money credit) {}
}
