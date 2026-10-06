package com.bradox.erp.accounting.service.domain.create;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request to set a company's opening balances. GL lines post as one balanced journal entry on
 * the Opening journal (OBE plug). Partner rows create opening invoices/bills or opening
 * unallocated payments. When {@code replace} is true, existing opening GL/docs/payments are
 * reversed first when no active allocations or credit notes block the replace.
 */
public class OpeningBalanceCommand {
    @NotNull
    private final UUID companyId;
    @NotNull
    private final LocalDate date;
    private final String currencyCode;
    private final boolean replace;
    @Valid
    private final List<OpeningBalanceLine> lines;
    @Valid
    private final List<OpeningPartnerLine> customerLines;
    @Valid
    private final List<OpeningPartnerLine> vendorLines;

    public OpeningBalanceCommand(UUID companyId, LocalDate date, String currencyCode,
                                 boolean replace, List<OpeningBalanceLine> lines) {
        this(companyId, date, currencyCode, replace, lines, List.of(), List.of());
    }

    public OpeningBalanceCommand(UUID companyId, LocalDate date, String currencyCode,
                                 boolean replace, List<OpeningBalanceLine> lines,
                                 List<OpeningPartnerLine> customerLines,
                                 List<OpeningPartnerLine> vendorLines) {
        this.companyId = companyId;
        this.date = date;
        this.currencyCode = currencyCode;
        this.replace = replace;
        this.lines = lines != null ? lines : List.of();
        this.customerLines = customerLines != null ? customerLines : List.of();
        this.vendorLines = vendorLines != null ? vendorLines : List.of();
    }

    public UUID getCompanyId() { return companyId; }
    public LocalDate getDate() { return date; }
    public String getCurrencyCode() { return currencyCode; }
    public boolean isReplace() { return replace; }
    public List<OpeningBalanceLine> getLines() { return lines; }
    public List<OpeningPartnerLine> getCustomerLines() { return customerLines; }
    public List<OpeningPartnerLine> getVendorLines() { return vendorLines; }
}
