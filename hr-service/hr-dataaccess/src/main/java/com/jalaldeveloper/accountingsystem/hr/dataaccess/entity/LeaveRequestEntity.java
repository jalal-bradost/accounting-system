package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_time_off_request")
public class LeaveRequestEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "time_off_type_id", nullable = false) private UUID timeOffTypeId;
    @Column(name = "date_from", nullable = false) private LocalDate dateFrom;
    @Column(name = "date_to", nullable = false) private LocalDate dateTo;
    @Column(name = "number_of_days", nullable = false, precision = 8, scale = 2) private BigDecimal numberOfDays;
    @Column(nullable = false, length = 32) private String state;
    @Column(length = 512) private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getTimeOffTypeId() { return timeOffTypeId; }
    public void setTimeOffTypeId(UUID timeOffTypeId) { this.timeOffTypeId = timeOffTypeId; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
    public BigDecimal getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(BigDecimal numberOfDays) { this.numberOfDays = numberOfDays; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
