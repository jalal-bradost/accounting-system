package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_inspection}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_inspection")
public class InspectionEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "order_id", nullable = false)
    public UUID orderId;
    @Column(name = "template_id")
    public UUID templateId;
    @Column(name = "performed_by")
    public String performedBy;
    @Column(name = "started_at", nullable = false)
    public Instant startedAt;
    @Column(name = "signed_off_at")
    public Instant signedOffAt;
    @Column(name = "signed_off_by")
    public String signedOffBy;
    @Column(name = "odometer_km")
    public Integer odometerKm;
    @Column(name = "notes")
    public String notes;
}
