package com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "contacts_payment_terms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "name"}))
public class PaymentTermsEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "days_net", nullable = false)
    private int daysNet;

    @Column(name = "discount_days", nullable = false)
    private int discountDays;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    public PaymentTermsEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDaysNet() { return daysNet; }
    public void setDaysNet(int daysNet) { this.daysNet = daysNet; }
    public int getDiscountDays() { return discountDays; }
    public void setDiscountDays(int discountDays) { this.discountDays = discountDays; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
}
