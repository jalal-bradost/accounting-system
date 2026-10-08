package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_labor_guide}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_labor_guide")
public class LaborGuideEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "code", nullable = false)
    public String code;
    @Column(name = "description_en")
    public String descriptionEn;
    @Column(name = "description_ar")
    public String descriptionAr;
    @Column(name = "description_ku")
    public String descriptionKu;
    @Column(name = "labor_category_id")
    public UUID laborCategoryId;
    @Column(name = "standard_minutes", nullable = false)
    public int standardMinutes;
    @Column(name = "make")
    public String make;
    @Column(name = "model")
    public String model;
    @Column(name = "year_from")
    public Integer yearFrom;
    @Column(name = "year_to")
    public Integer yearTo;
    @Column(name = "active", nullable = false)
    public boolean active;
}
