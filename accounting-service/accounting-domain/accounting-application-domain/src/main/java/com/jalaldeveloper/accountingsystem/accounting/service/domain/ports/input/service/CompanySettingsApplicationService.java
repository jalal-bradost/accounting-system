package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import java.time.LocalDate;
import java.util.UUID;

public interface CompanySettingsApplicationService {

    void setPeriodLockDate(UUID companyId, LocalDate periodLockDate);
}
