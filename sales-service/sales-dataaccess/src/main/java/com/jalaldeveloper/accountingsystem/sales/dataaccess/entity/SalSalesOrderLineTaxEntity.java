package com.jalaldeveloper.accountingsystem.sales.dataaccess.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "sal_sales_order_line_tax")
public class SalSalesOrderLineTaxEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sal_solt_line"))
    private SalSalesOrderLineEntity line;

    @Column(name = "tax_id", nullable = false)
    private UUID taxId;

    @Column(nullable = false)
    private int sequence;

    public SalSalesOrderLineTaxEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SalSalesOrderLineEntity getLine() { return line; }
    public void setLine(SalSalesOrderLineEntity line) { this.line = line; }
    public UUID getTaxId() { return taxId; }
    public void setTaxId(UUID taxId) { this.taxId = taxId; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
}
