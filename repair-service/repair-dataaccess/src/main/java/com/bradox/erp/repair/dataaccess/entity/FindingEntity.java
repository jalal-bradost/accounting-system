package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_finding}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_finding")
public class FindingEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "order_id", nullable = false)
    public UUID orderId;
    @Column(name = "severity", nullable = false)
    public String severity;
    @Column(name = "description", nullable = false)
    public String description;
    @Column(name = "cause")
    public String cause;
    @Column(name = "recommended_action")
    public String recommendedAction;
    @Column(name = "customer_visible", nullable = false)
    public boolean customerVisible;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "created_by")
    public String createdBy;
}
