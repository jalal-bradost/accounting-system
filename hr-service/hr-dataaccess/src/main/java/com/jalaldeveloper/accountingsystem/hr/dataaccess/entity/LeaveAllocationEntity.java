package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_time_off_allocation")
public class LeaveAllocationEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "time_off_type_id", nullable = false) private UUID timeOffTypeId;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "number_of_days", nullable = false, precision = 8, scale = 2) private BigDecimal numberOfDays;
    @Column(name = "allocation_type", nullable = false, length = 32) private String allocationType;
    @Column(nullable = false, length = 32) private String state;
    @Column(name = "date_from", nullable = false) private LocalDate dateFrom;
    @Column(name = "date_to", nullable = false) private LocalDate dateTo;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getTimeOffTypeId() { return timeOffTypeId; }
    public void setTimeOffTypeId(UUID timeOffTypeId) { this.timeOffTypeId = timeOffTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(BigDecimal numberOfDays) { this.numberOfDays = numberOfDays; }
    public String getAllocationType() { return allocationType; }
    public void setAllocationType(String allocationType) { this.allocationType = allocationType; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
}
