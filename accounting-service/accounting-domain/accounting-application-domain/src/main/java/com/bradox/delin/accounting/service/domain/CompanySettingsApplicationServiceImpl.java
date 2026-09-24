package com.bradox.delin.accounting.service.domain;

import com.bradox.delin.accounting.service.domain.ports.input.service.CompanySettingsApplicationService;
import com.bradox.delin.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.bradox.delin.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.bradox.delin.domain.core.exception.AccountingDomainException;
import com.bradox.delin.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
class CompanySettingsApplicationServiceImpl implements CompanySettingsApplicationService {

    private final CompanyLockDatePort companyLockDatePort;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    CompanySettingsApplicationServiceImpl(CompanyLockDatePort companyLockDatePort,
                                          FiscalPeriodRepository fiscalPeriodRepository) {
        this.companyLockDatePort = companyLockDatePort;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    @Override
    @Transactional
    public void setPeriodLockDate(UUID companyId, LocalDate periodLockDate) {
        CompanyId cid = new CompanyId(companyId);
        LocalDate clamped = clampLockDate(cid, periodLockDate);
        companyLockDatePort.setPeriodLockDate(cid, clamped);
    }

    LocalDate clampLockDate(CompanyId companyId, LocalDate requested) {
        LocalDate minLock = fiscalPeriodRepository.findLatestClosedPeriodEndDate(companyId).orElse(null);
        if (minLock == null) {
            return requested;
        }
        if (requested == null || requested.isBefore(minLock)) {
            if (requested == null) {
                throw new AccountingDomainException(
                        "Cannot clear period lock: fiscal periods are closed through " + minLock + ".");
            }
            throw new AccountingDomainException(
                    "Period lock cannot be earlier than last closed period end " + minLock + ".");
        }
        return requested;
    }
}
