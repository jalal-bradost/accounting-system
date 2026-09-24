package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.bradox.erp.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Shared posting-date rules: closed fiscal periods and inclusive period lock date.
 */
@Component
public class PeriodPostingGuard {

    private final CompanyLockDatePort companyLockDatePort;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    public PeriodPostingGuard(CompanyLockDatePort companyLockDatePort,
                              FiscalPeriodRepository fiscalPeriodRepository) {
        this.companyLockDatePort = companyLockDatePort;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    public void assertDatePostable(UUID companyId, LocalDate date) {
        assertDatePostable(new CompanyId(companyId), date);
    }

    public void assertDatePostable(CompanyId companyId, LocalDate date) {
        if (date == null) {
            throw new AccountingDomainException("Document date is required.");
        }
        companyLockDatePort.getPeriodLockDate(companyId).ifPresent(lockDate -> {
            if (!date.isAfter(lockDate)) {
                throw new AccountingDomainException(
                        "Cannot post: date " + date + " is on or before period lock date " + lockDate + ".");
            }
        });
        fiscalPeriodRepository.findPeriodContaining(companyId, date).ifPresent(period -> {
            if (!period.open()) {
                throw new AccountingDomainException(
                        "Cannot post: fiscal period " + period.startDate() + "–" + period.endDate() + " is closed.");
            }
        });
    }
}
