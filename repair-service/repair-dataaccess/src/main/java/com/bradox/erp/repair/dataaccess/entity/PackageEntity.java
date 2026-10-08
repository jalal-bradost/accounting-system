package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_package}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_package")
public class PackageEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "code")
    public String code;
    @Column(name = "name", nullable = false)
    public String name;
    @Column(name = "category")
    public String category;
    @Column(name = "active", nullable = false)
    public boolean active;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "created_by")
    public String createdBy;
}
