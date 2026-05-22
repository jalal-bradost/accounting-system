package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CompanyCurrencyApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.CurrencyRow;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.PageResult;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository.RateLine;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class CompanyCurrencyApplicationServiceImpl implements CompanyCurrencyApplicationService {

    private final CompanyCurrencyRepository companyCurrencyRepository;

    CompanyCurrencyApplicationServiceImpl(CompanyCurrencyRepository companyCurrencyRepository) {
        this.companyCurrencyRepository = companyCurrencyRepository;
    }

    @Override
    public PageResult listCurrencies(UUID companyId, String query, int page, int size) {
        return companyCurrencyRepository.search(new CompanyId(companyId), query, page, size);
    }

    @Override
    @Transactional
    public CurrencyRow createCurrency(
            UUID companyId,
            String code,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean active) {
        CompanyId cid = new CompanyId(companyId);
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("Currency code must be exactly 3 letters");
        }
        if (companyCurrencyRepository.existsByCompanyAndCodeIgnoreCase(cid, normalized)) {
            throw new IllegalArgumentException("Currency already exists: " + normalized);
        }
        if (ratePerBase == null || ratePerBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        UUID id = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        // insert() seeds the first dated rate line as well.
        companyCurrencyRepository.insert(
                id, cid, normalized, symbol.trim(), name.trim(), ratePerBase, false, active, today);
        return companyCurrencyRepository
                .findByCompanyAndId(cid, id)
                .orElseThrow(() -> new IllegalStateException("Failed to load created currency"));
    }

    @Override
    @Transactional
    public CurrencyRow updateCurrency(
            UUID companyId,
            UUID id,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean active) {
        CompanyId cid = new CompanyId(companyId);
        CurrencyRow existing =
                companyCurrencyRepository
                        .findByCompanyAndId(cid, id)
                        .orElseThrow(() -> new IllegalArgumentException("Currency not found"));
        if (existing.baseCurrency()) {
            if (ratePerBase == null || ratePerBase.compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Base currency rate must remain 1");
            }
            if (!active) {
                throw new IllegalArgumentException("Base currency cannot be deactivated");
            }
        } else if (ratePerBase == null || ratePerBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }

        // Persist a dated rate line for today when the rate changes — prior lines
        // are kept verbatim so historical postings still resolve correctly.
        LocalDate today = LocalDate.now();
        if (!existing.baseCurrency() && existing.ratePerBase().compareTo(ratePerBase) != 0) {
            companyCurrencyRepository.upsertRate(id, today, ratePerBase);
        }
        LocalDate cachedLastUpdated = recomputeLastRateUpdated(id, today, existing.lastRateUpdated());
        companyCurrencyRepository.update(
                cid, id, symbol.trim(), name.trim(), ratePerBase, active, cachedLastUpdated);
        return companyCurrencyRepository
                .findByCompanyAndId(cid, id)
                .orElseThrow(() -> new IllegalStateException("Failed to load updated currency"));
    }

    @Override
    public List<RateLine> listRates(UUID companyId, UUID currencyId) {
        // Currency must belong to the company, otherwise we don't expose its history.
        companyCurrencyRepository
                .findByCompanyAndId(new CompanyId(companyId), currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found"));
        return companyCurrencyRepository.listRates(currencyId);
    }

    @Override
    @Transactional
    public RateLine addRate(
            UUID companyId, UUID currencyId, LocalDate effectiveDate, BigDecimal rate) {
        CompanyId cid = new CompanyId(companyId);
        CurrencyRow existing =
                companyCurrencyRepository
                        .findByCompanyAndId(cid, currencyId)
                        .orElseThrow(() -> new IllegalArgumentException("Currency not found"));
        if (existing.baseCurrency()) {
            if (rate == null || rate.compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Base currency rate must always be 1");
            }
        } else if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("Effective date is required");
        }

        RateLine line = companyCurrencyRepository.upsertRate(currencyId, effectiveDate, rate);

        // Refresh the denormalized "current rate" / "last rate updated" cache. The
        // current rate is the most recent line whose effective_date ≤ today, while
        // the displayed "last rate updated" is the highest-dated line in history.
        LocalDate today = LocalDate.now();
        BigDecimal currentRate =
                companyCurrencyRepository
                        .findEffectiveRate(currencyId, today)
                        .map(RateLine::rate)
                        .orElse(existing.ratePerBase());
        LocalDate lastUpdated = recomputeLastRateUpdated(currencyId, today, existing.lastRateUpdated());
        companyCurrencyRepository.update(
                cid,
                currencyId,
                existing.symbol(),
                existing.name(),
                currentRate,
                existing.active(),
                lastUpdated);
        return line;
    }

    @Override
    public Optional<RateLine> rateOn(UUID companyId, UUID currencyId, LocalDate date) {
        companyCurrencyRepository
                .findByCompanyAndId(new CompanyId(companyId), currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found"));
        return companyCurrencyRepository.findEffectiveRate(currencyId, date);
    }

    private LocalDate recomputeLastRateUpdated(UUID currencyId, LocalDate today, LocalDate fallback) {
        return companyCurrencyRepository
                .findLatestRate(currencyId)
                .map(RateLine::effectiveDate)
                .orElse(fallback != null ? fallback : today);
    }
}
