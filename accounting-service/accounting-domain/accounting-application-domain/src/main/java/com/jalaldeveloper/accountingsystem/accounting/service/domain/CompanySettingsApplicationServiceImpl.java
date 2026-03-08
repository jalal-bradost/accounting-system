package com.jalaldeveloper.accountingsystem.accounting.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CompanySettingsApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
class CompanySettingsApplicationServiceImpl implements CompanySettingsApplicationService {

    private final CompanyLockDatePort companyLockDatePort;

    CompanySettingsApplicationServiceImpl(CompanyLockDatePort companyLockDatePort) {
        this.companyLockDatePort = companyLockDatePort;
    }

    @Override
    public void setPeriodLockDate(UUID companyId, LocalDate periodLockDate) {
        companyLockDatePort.setPeriodLockDate(new CompanyId(companyId), periodLockDate);
    }
}
