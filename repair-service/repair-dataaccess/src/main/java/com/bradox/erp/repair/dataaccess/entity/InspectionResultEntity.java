package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_inspection_result}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_inspection_result")
public class InspectionResultEntity {

    @Id
    public UUID id;
    @Column(name = "inspection_id", nullable = false)
    public UUID inspectionId;
    @Column(name = "sequence", nullable = false)
    public int sequence;
    @Column(name = "section", nullable = false)
    public String section;
    @Column(name = "item_label", nullable = false)
    public String itemLabel;
    @Column(name = "result", nullable = false)
    public String result;
    @Column(name = "note")
    public String note;
    @Column(name = "photo_document_id")
    public UUID photoDocumentId;
    @Column(name = "recommended_line_id")
    public UUID recommendedLineId;
}
