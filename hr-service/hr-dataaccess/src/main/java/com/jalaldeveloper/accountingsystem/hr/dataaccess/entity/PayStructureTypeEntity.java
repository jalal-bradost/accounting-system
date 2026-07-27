package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "pay_structure_type")
public class PayStructureTypeEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "scheduled_pay", nullable = false, length = 32)
    private String scheduledPay;

    @Column(name = "wage_type", nullable = false, length = 32)
    private String wageType;

    @Column(name = "working_schedule_id")
    private UUID workingScheduleId;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "pay_structure_id")
    private UUID payStructureId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public PayStructureTypeEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScheduledPay() { return scheduledPay; }
    public void setScheduledPay(String scheduledPay) { this.scheduledPay = scheduledPay; }
    public String getWageType() { return wageType; }
    public void setWageType(String wageType) { this.wageType = wageType; }
    public UUID getWorkingScheduleId() { return workingScheduleId; }
    public void setWorkingScheduleId(UUID workingScheduleId) { this.workingScheduleId = workingScheduleId; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public UUID getPayStructureId() { return payStructureId; }
    public void setPayStructureId(UUID payStructureId) { this.payStructureId = payStructureId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
