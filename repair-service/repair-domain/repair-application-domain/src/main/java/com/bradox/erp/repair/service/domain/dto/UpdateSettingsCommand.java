package com.bradox.erp.repair.service.domain.dto;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSettingsCommand(BigDecimal defaultHourlyRate, BigDecimal verbalLimitAdvisor, BigDecimal verbalLimitSupervisor,
                                    @Min(0) Integer warrantyDays, @Min(0) Integer warrantyKm, BigDecimal emergencyStartLimit,
                                    @Min(1) Integer quoteValidityDays, BigDecimal advisorDiscountLimitPercent,
                                    @Min(0) Integer comebackDays, BigDecimal partsTolerancePercent,
                                    UUID defaultInspectionTemplateId, UUID defaultQualityTemplateId) {
}
