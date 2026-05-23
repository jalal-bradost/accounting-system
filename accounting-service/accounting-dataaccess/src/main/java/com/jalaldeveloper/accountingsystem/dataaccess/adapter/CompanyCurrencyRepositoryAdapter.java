package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CompanyCurrencyRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.CompanyCurrencyEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.CurrencyRateEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.CompanyCurrencyJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.CurrencyRateJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CompanyCurrencyRepositoryAdapter implements CompanyCurrencyRepository {

    private final CompanyCurrencyJpaRepository jpaRepository;
    private final CurrencyRateJpaRepository rateJpaRepository;

    public CompanyCurrencyRepositoryAdapter(
            CompanyCurrencyJpaRepository jpaRepository,
            CurrencyRateJpaRepository rateJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.rateJpaRepository = rateJpaRepository;
    }

    @Override
    public PageResult search(CompanyId companyId, String query, int pageZeroBased, int pageSize) {
        String q = query == null || query.isBlank() ? null : query.trim();
        Page<CompanyCurrencyEntity> page =
                jpaRepository.search(companyId.getId(), q, PageRequest.of(pageZeroBased, pageSize));
        List<CurrencyRow> rows = page.getContent().stream().map(this::toRow).toList();
        return new PageResult(
                rows,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    public Optional<CurrencyRow> findByCompanyAndId(CompanyId companyId, UUID id) {
        return jpaRepository.findByCompanyIdAndId(companyId.getId(), id).map(this::toRow);
    }

    @Override
    public Optional<CurrencyRow> findByCompanyAndCode(CompanyId companyId, String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return jpaRepository
                .findByCompanyIdAndCodeIgnoreCase(companyId.getId(), code.trim())
                .map(this::toRow);
    }

    @Override
    public Optional<CurrencyRow> findBaseCurrency(CompanyId companyId) {
        return jpaRepository.findByCompanyIdOrderByBaseCurrencyDescCodeAsc(companyId.getId()).stream()
                .filter(CompanyCurrencyEntity::isBaseCurrency)
                .findFirst()
                .map(this::toRow);
    }

    @Override
    public boolean existsByCompanyId(CompanyId companyId) {
        return jpaRepository.existsByCompanyId(companyId.getId());
    }

    @Override
    public boolean existsByCompanyAndCodeIgnoreCase(CompanyId companyId, String code) {
        return jpaRepository.findByCompanyIdAndCodeIgnoreCase(companyId.getId(), code).isPresent();
    }

    @Override
    @Transactional
    public void insert(
            UUID id,
            CompanyId companyId,
            String code,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean baseCurrency,
            boolean active,
            LocalDate lastRateUpdated) {
        CompanyCurrencyEntity e = new CompanyCurrencyEntity();
        e.setId(id);
        e.setCompanyId(companyId.getId());
        e.setCode(code);
        e.setSymbol(symbol);
        e.setName(name);
        e.setRatePerBase(ratePerBase);
        e.setBaseCurrency(baseCurrency);
        e.setActive(active);
        e.setLastRateUpdated(lastRateUpdated);
        jpaRepository.save(e);
        // Anchor the history with the seeded rate so historical lookups never miss.
        upsertRate(id, lastRateUpdated == null ? LocalDate.now() : lastRateUpdated, ratePerBase);
    }

    @Override
    @Transactional
    public void update(
            CompanyId companyId,
            UUID id,
            String symbol,
            String name,
            BigDecimal ratePerBase,
            boolean active,
            LocalDate lastRateUpdated) {
        CompanyCurrencyEntity e =
                jpaRepository
                        .findByCompanyIdAndId(companyId.getId(), id)
                        .orElseThrow(() -> new IllegalArgumentException("Currency not found"));
        e.setSymbol(symbol);
        e.setName(name);
        e.setRatePerBase(ratePerBase);
        e.setActive(active);
        e.setLastRateUpdated(lastRateUpdated);
        jpaRepository.save(e);
    }

    @Override
    public List<RateLine> listRates(UUID currencyId) {
        return rateJpaRepository.findByCurrencyIdOrderByEffectiveDateDesc(currencyId).stream()
                .map(this::toRateLine)
                .toList();
    }

    @Override
    @Transactional
    public RateLine upsertRate(UUID currencyId, LocalDate effectiveDate, BigDecimal rate) {
        CurrencyRateEntity entity =
                rateJpaRepository
                        .findByCurrencyIdAndEffectiveDate(currencyId, effectiveDate)
                        .orElseGet(
                                () -> {
                                    CurrencyRateEntity n = new CurrencyRateEntity();
                                    n.setId(UUID.randomUUID());
                                    n.setCurrencyId(currencyId);
                                    n.setEffectiveDate(effectiveDate);
                                    return n;
                                });
        entity.setRate(rate);
        rateJpaRepository.save(entity);
        return toRateLine(entity);
    }

    @Override
    public Optional<RateLine> findEffectiveRate(UUID currencyId, LocalDate asOf) {
        return rateJpaRepository
                .findFirstByCurrencyIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        currencyId, asOf)
                .map(this::toRateLine);
    }

    @Override
    public Optional<RateLine> findLatestRate(UUID currencyId) {
        return rateJpaRepository
                .findFirstByCurrencyIdOrderByEffectiveDateDesc(currencyId)
                .map(this::toRateLine);
    }

    private CurrencyRow toRow(CompanyCurrencyEntity e) {
        return new CurrencyRow(
                e.getId(),
                e.getCode(),
                e.getSymbol(),
                e.getName(),
                e.getLastRateUpdated(),
                e.getRatePerBase(),
                e.isBaseCurrency(),
                e.isActive());
    }

    private RateLine toRateLine(CurrencyRateEntity e) {
        return new RateLine(e.getId(), e.getCurrencyId(), e.getEffectiveDate(), e.getRate());
    }
}
