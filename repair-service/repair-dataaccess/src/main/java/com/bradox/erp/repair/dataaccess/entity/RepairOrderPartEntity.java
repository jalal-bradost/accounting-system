package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Column holder for {@code rep_order_part}. */
@Entity
@Table(name = "rep_order_part")
public class RepairOrderPartEntity {

    @Id
    public UUID id;
    @Column(name = "sequence", nullable = false)
    public int sequence;
    @Column(name = "product_id", nullable = false)
    public UUID productId;
    @Column(name = "qty", nullable = false)
    public BigDecimal qty;
}
