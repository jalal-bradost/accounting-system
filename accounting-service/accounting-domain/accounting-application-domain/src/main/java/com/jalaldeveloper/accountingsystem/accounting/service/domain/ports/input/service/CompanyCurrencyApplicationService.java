package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.CurrencyRow;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.PageResult;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.RateLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyCurrencyApplicationService {

    PageResult listCurrencies(UUID companyId, String query, int page, int size);

    CurrencyRow createCurrency(
            UUID companyId,
            String code,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean active);

    /**
     * Updates editable metadata. Rate changes (when {@code ratePerBase} differs from
     * today's effective rate) are persisted as a NEW dated line for {@link LocalDate#now()};
     * prior lines are never deleted.
     */
    CurrencyRow updateCurrency(
            UUID companyId,
            UUID id,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean active);

    /** Returns the full history (newest-first). */
    List<RateLine> listRates(UUID companyId, UUID currencyId);

    /** Adds a dated rate line, preserving every existing one. */
    RateLine addRate(UUID companyId, UUID currencyId, LocalDate effectiveDate, BigDecimal rate);

    /** Resolves the rate that applies on a given transaction date. */
    Optional<RateLine> rateOn(UUID companyId, UUID currencyId, LocalDate date);
}
