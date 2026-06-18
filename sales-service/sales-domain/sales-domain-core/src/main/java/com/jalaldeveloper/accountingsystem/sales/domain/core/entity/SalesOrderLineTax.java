package com.jalaldeveloper.accountingsystem.sales.domain.core.entity;

import java.util.UUID;

public class SalesOrderLineTax {

    private UUID id;
    private UUID taxId;
    private int sequence;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTaxId() { return taxId; }
    public void setTaxId(UUID taxId) { this.taxId = taxId; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
}
