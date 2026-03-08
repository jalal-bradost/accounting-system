package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.FiscalPeriodApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.FiscalPeriodRepository.FiscalPeriodInfo;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
class FiscalPeriodApplicationServiceImpl implements FiscalPeriodApplicationService {

    private final FiscalPeriodRepository fiscalPeriodRepository;

    FiscalPeriodApplicationServiceImpl(FiscalPeriodRepository fiscalPeriodRepository) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    @Override
    public FiscalPeriodInfo createPeriod(UUID companyId, LocalDate startDate, LocalDate endDate, boolean open) {
        return fiscalPeriodRepository.create(new CompanyId(companyId), startDate, endDate, open);
    }

    @Override
    public List<FiscalPeriodInfo> listPeriods(UUID companyId) {
        return fiscalPeriodRepository.findByCompanyIdOrderByStartDateDesc(new CompanyId(companyId));
    }
}
