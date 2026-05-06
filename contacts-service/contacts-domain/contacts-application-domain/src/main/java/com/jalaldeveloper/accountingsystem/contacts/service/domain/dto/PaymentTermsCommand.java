package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentTermsCommand {
    private UUID companyId;
    @NotBlank private String name;
    @PositiveOrZero private int daysNet;
    @PositiveOrZero private int discountDays;
    private BigDecimal discountPercent;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public int getDaysNet() { return daysNet; }
    public void setDaysNet(int v) { this.daysNet = v; }
    public int getDiscountDays() { return discountDays; }
    public void setDiscountDays(int v) { this.discountDays = v; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal v) { this.discountPercent = v; }
}
