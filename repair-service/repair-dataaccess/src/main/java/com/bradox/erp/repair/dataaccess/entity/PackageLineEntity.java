package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_package_line}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_package_line")
public class PackageLineEntity {

    @Id
    public UUID id;
    @Column(name = "package_id", nullable = false)
    public UUID packageId;
    @Column(name = "sequence", nullable = false)
    public int sequence;
    @Column(name = "line_type", nullable = false)
    public String lineType;
    @Column(name = "product_id")
    public UUID productId;
    @Column(name = "labor_guide_id")
    public UUID laborGuideId;
    @Column(name = "description")
    public String description;
    @Column(name = "qty", nullable = false)
    public BigDecimal qty;
    @Column(name = "standard_minutes")
    public Integer standardMinutes;
}
