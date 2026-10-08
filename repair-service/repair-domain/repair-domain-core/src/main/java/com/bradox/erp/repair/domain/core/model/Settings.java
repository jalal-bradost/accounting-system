package com.bradox.erp.repair.domain.core.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Company-wide repair settings. The defaults are placeholders the Manager must review before go-live (D7, D10). */
public record Settings(UUID id, UUID companyId, BigDecimal defaultHourlyRate, BigDecimal verbalLimitAdvisor,
                       BigDecimal verbalLimitSupervisor, int warrantyDays, int warrantyKm, BigDecimal emergencyStartLimit,
                       int quoteValidityDays, BigDecimal advisorDiscountLimitPercent, int comebackDays,
                       BigDecimal partsTolerancePercent, UUID defaultInspectionTemplateId, UUID defaultQualityTemplateId) {

    public static Settings defaults(UUID companyId) {
        return new Settings(UUID.randomUUID(), companyId, null, BigDecimal.valueOf(250), BigDecimal.valueOf(1000), 30, 1000,
                BigDecimal.valueOf(100), 7, BigDecimal.valueOf(5), 30, BigDecimal.valueOf(10), null, null);
    }
}
