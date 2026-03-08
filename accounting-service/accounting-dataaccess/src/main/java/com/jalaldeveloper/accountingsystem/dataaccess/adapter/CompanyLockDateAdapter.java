package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.CompanySettingsEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.CompanySettingsJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class CompanyLockDateAdapter implements CompanyLockDatePort {

    private final CompanySettingsJpaRepository settingsRepository;

    public CompanyLockDateAdapter(CompanySettingsJpaRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public Optional<LocalDate> getPeriodLockDate(CompanyId companyId) {
        return settingsRepository.findById(companyId.getId())
                .map(CompanySettingsEntity::getPeriodLockDate)
                .filter(d -> d != null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void setPeriodLockDate(CompanyId companyId, LocalDate periodLockDate) {
        CompanySettingsEntity settings = settingsRepository.findById(companyId.getId())
                .orElseGet(() -> {
                    CompanySettingsEntity e = new CompanySettingsEntity();
                    e.setCompanyId(companyId.getId());
                    return e;
                });
        settings.setPeriodLockDate(periodLockDate);
        settingsRepository.save(settings);
    }
}
