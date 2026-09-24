package com.bradox.delin.dataaccess.adapter;

import com.bradox.delin.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.bradox.delin.dataaccess.entity.FiscalPeriodEntity;
import com.bradox.delin.dataaccess.repository.FiscalPeriodJpaRepository;
import com.bradox.delin.domain.core.exception.AccountingDomainException;
import com.bradox.delin.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FiscalPeriodRepositoryAdapter implements FiscalPeriodRepository {

    private final FiscalPeriodJpaRepository jpaRepository;

    public FiscalPeriodRepositoryAdapter(FiscalPeriodJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<FiscalPeriodInfo> findPeriodContaining(CompanyId companyId, LocalDate date) {
        return jpaRepository.findByCompanyIdAndDateBetweenStartAndEnd(companyId.getId(), date)
                .map(this::toInfo);
    }

    @Override
    public Optional<FiscalPeriodInfo> findById(CompanyId companyId, UUID periodId) {
        return jpaRepository.findById(periodId)
                .filter(e -> e.getCompanyId().equals(companyId.getId()))
                .map(this::toInfo);
    }

    @Override
    public List<FiscalPeriodInfo> findByCompanyIdOrderByStartDateDesc(CompanyId companyId) {
        return jpaRepository.findByCompanyIdOrderByStartDateDesc(companyId.getId()).stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FiscalPeriodInfo create(CompanyId companyId, LocalDate startDate, LocalDate endDate) {
        FiscalPeriodEntity e = new FiscalPeriodEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(companyId.getId());
        e.setStartDate(startDate);
        e.setEndDate(endDate);
        e.setOpen(true);
        e = jpaRepository.save(e);
        return toInfo(e);
    }

    @Override
    @Transactional
    public FiscalPeriodInfo close(CompanyId companyId, UUID periodId, UUID closedBy, Instant closedAt) {
        FiscalPeriodEntity e = jpaRepository.findById(periodId)
                .filter(row -> row.getCompanyId().equals(companyId.getId()))
                .orElseThrow(() -> new AccountingDomainException("Fiscal period not found: " + periodId));
        if (!e.isOpen()) {
            return toInfo(e);
        }
        e.setOpen(false);
        e.setClosedAt(closedAt != null ? closedAt : Instant.now());
        e.setClosedBy(closedBy);
        return toInfo(jpaRepository.save(e));
    }

    @Override
    public Optional<LocalDate> findLatestClosedPeriodEndDate(CompanyId companyId) {
        return jpaRepository.findByCompanyIdOrderByStartDateDesc(companyId.getId()).stream()
                .filter(e -> !e.isOpen())
                .map(FiscalPeriodEntity::getEndDate)
                .max(Comparator.naturalOrder());
    }

    private FiscalPeriodInfo toInfo(FiscalPeriodEntity e) {
        return new FiscalPeriodInfo(
                e.getId(),
                e.getStartDate(),
                e.getEndDate(),
                e.isOpen(),
                e.getClosedAt(),
                e.getClosedBy());
    }
}
