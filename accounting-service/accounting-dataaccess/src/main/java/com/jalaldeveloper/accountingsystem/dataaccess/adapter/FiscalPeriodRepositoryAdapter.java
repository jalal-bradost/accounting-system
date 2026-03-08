package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.FiscalPeriodEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.FiscalPeriodJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class FiscalPeriodRepositoryAdapter implements FiscalPeriodRepository {

    private final FiscalPeriodJpaRepository jpaRepository;

    public FiscalPeriodRepositoryAdapter(FiscalPeriodJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<FiscalPeriodInfo> findPeriodContaining(CompanyId companyId, LocalDate date) {
        return jpaRepository.findByCompanyIdAndDateBetweenStartAndEnd(companyId.getId(), date)
                .map(e -> new FiscalPeriodInfo(e.getId(), e.getStartDate(), e.getEndDate(), e.isOpen()));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public FiscalPeriodInfo create(CompanyId companyId, LocalDate startDate, LocalDate endDate, boolean open) {
        FiscalPeriodEntity e = new FiscalPeriodEntity();
        e.setId(java.util.UUID.randomUUID());
        e.setCompanyId(companyId.getId());
        e.setStartDate(startDate);
        e.setEndDate(endDate);
        e.setOpen(open);
        e = jpaRepository.save(e);
        return new FiscalPeriodInfo(e.getId(), e.getStartDate(), e.getEndDate(), e.isOpen());
    }
}
