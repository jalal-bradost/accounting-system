package com.bradox.erp.accounting.service.domain.create;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Append-only partner opening corrections. Positive amount increases the tracked opening
 * (customer owes more / we owe vendor more); negative decreases it. Posts against the
 * Opening Balance Adjustment equity account — never replaces prior openings.
 */
public class OpeningBalanceAdjustmentCommand {
    @NotNull
    private final UUID companyId;
    @NotNull
    private final LocalDate date;
    private final String currencyCode;
    @Valid
    private final List<OpeningPartnerLine> customerLines;
    @Valid
    private final List<OpeningPartnerLine> vendorLines;

    public OpeningBalanceAdjustmentCommand(UUID companyId, LocalDate date, String currencyCode,
                                           List<OpeningPartnerLine> customerLines,
                                           List<OpeningPartnerLine> vendorLines) {
        this.companyId = companyId;
        this.date = date;
        this.currencyCode = currencyCode;
        this.customerLines = customerLines != null ? customerLines : List.of();
        this.vendorLines = vendorLines != null ? vendorLines : List.of();
    }

    public UUID getCompanyId() { return companyId; }
    public LocalDate getDate() { return date; }
    public String getCurrencyCode() { return currencyCode; }
    public List<OpeningPartnerLine> getCustomerLines() { return customerLines; }
    public List<OpeningPartnerLine> getVendorLines() { return vendorLines; }
}
