package com.bradox.erp.project.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Column holder for {@code prj_task}. */
@Entity
@Table(name = "prj_task")
public class PrjTaskEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "project_id", nullable = false)
    public UUID projectId;
    @Column(name = "stage_id", nullable = false)
    public UUID stageId;
    @Column(name = "name", nullable = false)
    public String name;
    @Column(name = "description")
    public String description;
    @Column(name = "customer_partner_id")
    public UUID customerPartnerId;
    @Column(name = "assignee_username")
    public String assigneeUsername;
    @Column(name = "deadline")
    public LocalDate deadline;
    @Column(name = "priority", nullable = false)
    public int priority;
    @Column(name = "sequence", nullable = false)
    public int sequence;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
