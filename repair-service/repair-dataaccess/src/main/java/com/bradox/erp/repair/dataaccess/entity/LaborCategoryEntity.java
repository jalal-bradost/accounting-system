package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_labor_category}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_labor_category")
public class LaborCategoryEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "name", nullable = false)
    public String name;
    @Column(name = "hourly_rate")
    public BigDecimal hourlyRate;
    @Column(name = "active", nullable = false)
    public boolean active;
}
