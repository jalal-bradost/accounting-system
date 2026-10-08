package com.bradox.erp.repair.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Column holder for {@code rep_line}; the adapters map it to the domain model. */
@Entity
@Table(name = "rep_line")
public class LineEntity {

    @Id
    public UUID id;
    @Column(name = "company_id", nullable = false)
    public UUID companyId;
    @Column(name = "order_id", nullable = false)
    public UUID orderId;
    @Column(name = "line_type", nullable = false)
    public String lineType;
    @Column(name = "section_label")
    public String sectionLabel;
    @Column(name = "sequence", nullable = false)
    public int sequence;
    @Column(name = "product_id")
    public UUID productId;
    @Column(name = "description", nullable = false)
    public String description;
    @Column(name = "qty", nullable = false)
    public BigDecimal qty;
    @Column(name = "unit_price", nullable = false)
    public BigDecimal unitPrice;
    @Column(name = "discount_percent", nullable = false)
    public BigDecimal discountPercent;
    @Column(name = "status", nullable = false)
    public String status;
    @Column(name = "needs_discount_approval", nullable = false)
    public boolean needsDiscountApproval;
    @Column(name = "from_finding_id")
    public UUID fromFindingId;
    @Column(name = "from_package_id")
    public UUID fromPackageId;
    @Column(name = "labor_guide_id")
    public UUID laborGuideId;
    @Column(name = "pricing_mode")
    public String pricingMode;
    @Column(name = "standard_minutes")
    public Integer standardMinutes;
    @Column(name = "labor_rate")
    public BigDecimal laborRate;
    @Column(name = "assignee_employee_id")
    public UUID assigneeEmployeeId;
    @Column(name = "vendor_partner_id")
    public UUID vendorPartnerId;
    @Column(name = "vendor_cost")
    public BigDecimal vendorCost;
    @Column(name = "sublet_status")
    public String subletStatus;
    @Column(name = "disposition")
    public String disposition;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "created_by")
    public String createdBy;
}
