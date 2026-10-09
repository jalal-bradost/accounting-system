package com.bradox.erp.project.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Column holder for {@code prj_project}; the adapter maps it to the domain model. */
@Entity
@Table(name = "prj_project")
public class PrjProjectEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "name", nullable = false)
    public String name;
    @Column(name = "customer_partner_id")
    public UUID customerPartnerId;
    @Column(name = "manager_username")
    public String managerUsername;
    @Column(name = "start_date")
    public LocalDate startDate;
    @Column(name = "end_date")
    public LocalDate endDate;
    @Column(name = "color", nullable = false)
    public int color;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
