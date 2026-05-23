package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for company-scoped currencies and FX rates. */
public interface CompanyCurrencyRepository {

    record CurrencyRow(
            UUID id,
            String code,
            String symbol,
            String name,
            LocalDate lastRateUpdated,
            BigDecimal ratePerBase,
            boolean baseCurrency,
            boolean active) {}

    record PageResult(
            List<CurrencyRow> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {}

    /** A single dated rate-history line for a currency. */
    record RateLine(UUID id, UUID currencyId, LocalDate effectiveDate, BigDecimal rate) {}

    PageResult search(CompanyId companyId, String query, int pageZeroBased, int pageSize);

    Optional<CurrencyRow> findByCompanyAndId(CompanyId companyId, UUID id);

    Optional<CurrencyRow> findByCompanyAndCode(CompanyId companyId, String code);

    Optional<CurrencyRow> findBaseCurrency(CompanyId companyId);

    boolean existsByCompanyId(CompanyId companyId);

    boolean existsByCompanyAndCodeIgnoreCase(CompanyId companyId, String code);

    void insert(
            UUID id,
            CompanyId companyId,
            String code,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean baseCurrency,
            boolean active,
            LocalDate lastRateUpdated);

    void update(
            CompanyId companyId,
            UUID id,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean active,
            LocalDate lastRateUpdated);

    /** Newest-first dated history for the currency (preserves all prior lines). */
    List<RateLine> listRates(UUID currencyId);

    /**
     * Inserts (or upserts) a dated rate line. {@code effectiveDate} is the calendar
     * day the rate becomes the applicable one. If a line for that exact date exists,
     * its rate is overwritten — never deleted, never replaces older lines.
     */
    RateLine upsertRate(UUID currencyId, LocalDate effectiveDate, BigDecimal rate);

    /**
     * Returns the rate effective on the given date (the most recent line whose
     * effective_date ≤ {@code asOf}). Used by the accounting layer to convert
     * amounts at posting time, so historical lines must remain accurate.
     */
    Optional<RateLine> findEffectiveRate(UUID currencyId, LocalDate asOf);

    /** The single highest-dated line overall. Drives the {@code last_rate_updated} cache. */
    Optional<RateLine> findLatestRate(UUID currencyId);
}
