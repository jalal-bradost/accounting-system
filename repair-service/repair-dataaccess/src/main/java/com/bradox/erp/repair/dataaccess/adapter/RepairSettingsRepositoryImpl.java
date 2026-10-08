package com.bradox.erp.repair.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.dataaccess.entity.RepairSettingsEntity;
import com.bradox.erp.repair.dataaccess.repository.RepairSettingsJpaRepository;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.service.domain.ports.output.repository.SettingsRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RepairSettingsRepositoryImpl implements SettingsRepository {

    private final RepairSettingsJpaRepository jpa;

    public RepairSettingsRepositoryImpl(RepairSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Settings> find(CompanyId companyId) {
        return jpa.findByCompanyId(companyId.getId()).map(RepairSettingsRepositoryImpl::toDomain);
    }

    @Override
    public Settings save(Settings s) {
        RepairSettingsEntity e = jpa.findByCompanyId(s.companyId()).orElseGet(RepairSettingsEntity::new);
        e.id = s.id();
        e.companyId = s.companyId();
        e.defaultHourlyRate = s.defaultHourlyRate();
        e.verbalLimitAdvisor = s.verbalLimitAdvisor();
        e.verbalLimitSupervisor = s.verbalLimitSupervisor();
        e.warrantyDays = s.warrantyDays();
        e.warrantyKm = s.warrantyKm();
        e.emergencyStartLimit = s.emergencyStartLimit();
        e.quoteValidityDays = s.quoteValidityDays();
        e.advisorDiscountLimitPercent = s.advisorDiscountLimitPercent();
        e.comebackDays = s.comebackDays();
        e.partsTolerancePercent = s.partsTolerancePercent();
        e.defaultInspectionTemplateId = s.defaultInspectionTemplateId();
        e.defaultQualityTemplateId = s.defaultQualityTemplateId();
        return toDomain(jpa.saveAndFlush(e));
    }

    private static Settings toDomain(RepairSettingsEntity e) {
        return new Settings(e.id, e.companyId, e.defaultHourlyRate, e.verbalLimitAdvisor, e.verbalLimitSupervisor, e.warrantyDays,
                e.warrantyKm, e.emergencyStartLimit, e.quoteValidityDays, e.advisorDiscountLimitPercent, e.comebackDays,
                e.partsTolerancePercent, e.defaultInspectionTemplateId, e.defaultQualityTemplateId);
    }
}
