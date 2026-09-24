package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.settings.CompanyLockDatePort;
import com.bradox.erp.dataaccess.entity.CompanySettingsEntity;
import com.bradox.erp.dataaccess.repository.CompanySettingsJpaRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class CompanyLockDateAdapter implements CompanyLockDatePort {

    private final CompanySettingsJpaRepository settingsRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
    @Transactional
    public void setPeriodLockDate(CompanyId companyId, LocalDate periodLockDate) {
        CompanySettingsEntity settings = settingsRepository.findById(companyId.getId())
                .orElseGet(() -> {
                    CompanySettingsEntity e = new CompanySettingsEntity();
                    e.setCompanyId(companyId.getId());
                    return e;
                });
        settings.setPeriodLockDate(periodLockDate);
        settingsRepository.save(settings);

        // Keep platform_company.period_lock_date in sync for settings UI.
        entityManager.createNativeQuery(
                        "UPDATE platform_company SET period_lock_date = :lockDate WHERE id = :companyId")
                .setParameter("lockDate", periodLockDate)
                .setParameter("companyId", companyId.getId())
                .executeUpdate();
    }

    @Override
    public int getFiscalYearStartMonth(CompanyId companyId) {
        var results = entityManager.createNativeQuery(
                        "SELECT fiscal_year_start_month FROM platform_company WHERE id = :companyId")
                .setParameter("companyId", companyId.getId())
                .getResultList();
        if (results.isEmpty() || results.get(0) == null) {
            return 1;
        }
        int month = ((Number) results.get(0)).intValue();
        return month >= 1 && month <= 12 ? month : 1;
    }
}
