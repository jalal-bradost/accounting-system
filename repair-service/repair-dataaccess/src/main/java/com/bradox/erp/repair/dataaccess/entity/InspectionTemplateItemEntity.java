package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_inspection_template_item}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_inspection_template_item")
public class InspectionTemplateItemEntity {

    @Id
    public UUID id;
    @Column(name = "template_id", nullable = false)
    public UUID templateId;
    @Column(name = "section", nullable = false)
    public String section;
    @Column(name = "label", nullable = false)
    public String label;
    @Column(name = "sequence", nullable = false)
    public int sequence;
}
