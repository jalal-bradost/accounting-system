package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pay_contract")
public class PayContractEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "employee_type_id")
    private UUID employeeTypeId;

    @Column(name = "structure_id", nullable = false)
    private UUID structureId;

    @Column(name = "working_schedule_id", nullable = false)
    private UUID workingScheduleId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal wage;

    @Column(name = "wage_type", nullable = false, length = 32)
    private String wageType;

    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode;

    @Column(name = "date_start", nullable = false)
    private LocalDate dateStart;

    @Column(name = "date_end")
    private LocalDate dateEnd;

    @Column(nullable = false, length = 32)
    private String state;

    @Column(name = "attendance_based", nullable = false)
    private boolean attendanceBased;

    public PayContractEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getEmployeeTypeId() { return employeeTypeId; }
    public void setEmployeeTypeId(UUID employeeTypeId) { this.employeeTypeId = employeeTypeId; }
    public UUID getStructureId() { return structureId; }
    public void setStructureId(UUID structureId) { this.structureId = structureId; }
    public UUID getWorkingScheduleId() { return workingScheduleId; }
    public void setWorkingScheduleId(UUID workingScheduleId) { this.workingScheduleId = workingScheduleId; }
    public BigDecimal getWage() { return wage; }
    public void setWage(BigDecimal wage) { this.wage = wage; }
    public String getWageType() { return wageType; }
    public void setWageType(String wageType) { this.wageType = wageType; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public LocalDate getDateStart() { return dateStart; }
    public void setDateStart(LocalDate dateStart) { this.dateStart = dateStart; }
    public LocalDate getDateEnd() { return dateEnd; }
    public void setDateEnd(LocalDate dateEnd) { this.dateEnd = dateEnd; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public boolean isAttendanceBased() { return attendanceBased; }
    public void setAttendanceBased(boolean attendanceBased) { this.attendanceBased = attendanceBased; }
}
