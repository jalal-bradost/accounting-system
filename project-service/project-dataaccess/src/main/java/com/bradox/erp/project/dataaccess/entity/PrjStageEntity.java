package com.bradox.erp.project.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Column holder for {@code prj_stage}. */
@Entity
@Table(name = "prj_stage")
public class PrjStageEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "project_id", nullable = false)
    public UUID projectId;
    @Column(name = "name", nullable = false)
    public String name;
    @Column(name = "sequence", nullable = false)
    public int sequence;
}
