package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Column holder for {@code rep_order}; the adapter maps it to the domain model. */
@Entity
@Table(name = "rep_order")
public class RepairOrderEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "reference", nullable = false)
    public String reference;
    @Column(name = "customer_partner_id")
    public UUID customerPartnerId;
    @Column(name = "product_id")
    public UUID productId;
    @Column(name = "scheduled_date")
    public Instant scheduledDate;
    @Column(name = "under_warranty", nullable = false)
    public boolean underWarranty;
    @Column(name = "status", nullable = false)
    public String status;
    @Column(name = "sale_order_id")
    public UUID saleOrderId;
    @Column(name = "sale_order_name")
    public String saleOrderName;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderBy("sequence")
    public List<RepairOrderPartEntity> parts = new ArrayList<>();
}
