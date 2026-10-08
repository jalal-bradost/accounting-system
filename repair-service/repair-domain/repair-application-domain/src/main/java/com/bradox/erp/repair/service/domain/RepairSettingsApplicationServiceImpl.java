package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.service.domain.dto.UpdateSettingsCommand;
import com.bradox.erp.repair.service.domain.ports.input.SettingsApplicationService;
import com.bradox.erp.repair.service.domain.ports.output.repository.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Service
@Validated
class RepairSettingsApplicationServiceImpl implements SettingsApplicationService {

    private final RepairSupport support;
    private final SettingsRepository settings;
    private final RepairAccess access;
    private final AuditLogPort audit;

    RepairSettingsApplicationServiceImpl(RepairSupport support, SettingsRepository settings, RepairAccess access, AuditLogPort audit) {
        this.support = support;
        this.settings = settings;
        this.access = access;
        this.audit = audit;
    }

    @Override
    @Transactional
    public Settings get(CompanyId companyId) {
        access.require(RepairPermissions.ORDER_VIEW);
        return support.settings(companyId);
    }

    @Override
    @Transactional
    public Settings update(CompanyId companyId, UpdateSettingsCommand c) {
        access.require(RepairPermissions.SETTINGS_MANAGE);
        Settings s = support.settings(companyId);
        Settings saved = settings.save(new Settings(s.id(), s.companyId(),
                c.defaultHourlyRate() != null ? c.defaultHourlyRate() : s.defaultHourlyRate(),
                c.verbalLimitAdvisor() != null ? c.verbalLimitAdvisor() : s.verbalLimitAdvisor(),
                c.verbalLimitSupervisor() != null ? c.verbalLimitSupervisor() : s.verbalLimitSupervisor(),
                c.warrantyDays() != null ? c.warrantyDays() : s.warrantyDays(),
                c.warrantyKm() != null ? c.warrantyKm() : s.warrantyKm(),
                c.emergencyStartLimit() != null ? c.emergencyStartLimit() : s.emergencyStartLimit(),
                c.quoteValidityDays() != null ? c.quoteValidityDays() : s.quoteValidityDays(),
                c.advisorDiscountLimitPercent() != null ? c.advisorDiscountLimitPercent() : s.advisorDiscountLimitPercent(),
                c.comebackDays() != null ? c.comebackDays() : s.comebackDays(),
                c.partsTolerancePercent() != null ? c.partsTolerancePercent() : s.partsTolerancePercent(),
                c.defaultInspectionTemplateId() != null ? c.defaultInspectionTemplateId() : s.defaultInspectionTemplateId(),
                c.defaultQualityTemplateId() != null ? c.defaultQualityTemplateId() : s.defaultQualityTemplateId()));
        audit.recordBusinessEvent(companyId, "rep.settings", saved.id(), "Repair settings updated", Map.of("by", access.actorLabel()));
        return saved;
    }
}
