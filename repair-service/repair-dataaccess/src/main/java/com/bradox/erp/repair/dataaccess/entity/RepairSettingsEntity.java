package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_settings}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_settings")
public class RepairSettingsEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "default_hourly_rate")
    public BigDecimal defaultHourlyRate;
    @Column(name = "verbal_limit_advisor", nullable = false)
    public BigDecimal verbalLimitAdvisor;
    @Column(name = "verbal_limit_supervisor", nullable = false)
    public BigDecimal verbalLimitSupervisor;
    @Column(name = "warranty_days", nullable = false)
    public int warrantyDays;
    @Column(name = "warranty_km", nullable = false)
    public int warrantyKm;
    @Column(name = "emergency_start_limit", nullable = false)
    public BigDecimal emergencyStartLimit;
    @Column(name = "quote_validity_days", nullable = false)
    public int quoteValidityDays;
    @Column(name = "advisor_discount_limit_percent", nullable = false)
    public BigDecimal advisorDiscountLimitPercent;
    @Column(name = "comeback_days", nullable = false)
    public int comebackDays;
    @Column(name = "parts_tolerance_percent", nullable = false)
    public BigDecimal partsTolerancePercent;
    @Column(name = "default_inspection_template_id")
    public UUID defaultInspectionTemplateId;
    @Column(name = "default_quality_template_id")
    public UUID defaultQualityTemplateId;
}
