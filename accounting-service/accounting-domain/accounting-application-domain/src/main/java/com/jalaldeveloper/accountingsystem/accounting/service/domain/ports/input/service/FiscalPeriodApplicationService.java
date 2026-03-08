package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.FiscalPeriodRepository.FiscalPeriodInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FiscalPeriodApplicationService {

    FiscalPeriodInfo createPeriod(UUID companyId, LocalDate startDate, LocalDate endDate, boolean open);

    List<FiscalPeriodInfo> listPeriods(UUID companyId);
}
