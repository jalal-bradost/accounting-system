package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request to set a company's opening balances. The service posts a single balanced journal
 * entry on the dedicated "Opening" journal; any residual difference is plugged to the
 * Opening Balance Equity account. When {@code replace} is true, an existing posted opening
 * entry is reversed first.
 */
public class OpeningBalanceCommand {
    @NotNull
    private final UUID companyId;
    @NotNull
    private final LocalDate date;
    private final String currencyCode;
    private final boolean replace;
    @NotEmpty
    @Valid
    private final List<OpeningBalanceLine> lines;

    public OpeningBalanceCommand(UUID companyId, LocalDate date, String currencyCode,
                                 boolean replace, List<OpeningBalanceLine> lines) {
        this.companyId = companyId;
        this.date = date;
        this.currencyCode = currencyCode;
        this.replace = replace;
        this.lines = lines != null ? lines : List.of();
    }

    public UUID getCompanyId() { return companyId; }
    public LocalDate getDate() { return date; }
    public String getCurrencyCode() { return currencyCode; }
    public boolean isReplace() { return replace; }
    public List<OpeningBalanceLine> getLines() { return lines; }
}
