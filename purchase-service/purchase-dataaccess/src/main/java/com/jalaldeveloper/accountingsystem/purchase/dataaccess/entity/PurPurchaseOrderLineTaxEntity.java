package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "pur_purchase_order_line_tax")
public class PurPurchaseOrderLineTaxEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pur_polt_line"))
    private PurPurchaseOrderLineEntity line;

    @Column(name = "tax_id", nullable = false)
    private UUID taxId;

    @Column(nullable = false)
    private int sequence;

    public PurPurchaseOrderLineTaxEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PurPurchaseOrderLineEntity getLine() { return line; }
    public void setLine(PurPurchaseOrderLineEntity line) { this.line = line; }
    public UUID getTaxId() { return taxId; }
    public void setTaxId(UUID taxId) { this.taxId = taxId; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
}
