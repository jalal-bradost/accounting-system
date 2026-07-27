package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class GenerateAttendanceFromScheduleCommand {

    private UUID companyId;
    private UUID employeeId;
    @NotNull private UUID workingScheduleId;
    @NotNull private LocalDate fromDate;
    @NotNull private LocalDate toDate;
    private int defaultStartHour = 9;
    private int defaultStartMinute = 0;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID v) { this.employeeId = v; }
    public UUID getWorkingScheduleId() { return workingScheduleId; }
    public void setWorkingScheduleId(UUID v) { this.workingScheduleId = v; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate v) { this.fromDate = v; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate v) { this.toDate = v; }
    public int getDefaultStartHour() { return defaultStartHour; }
    public void setDefaultStartHour(int v) { this.defaultStartHour = v; }
    public int getDefaultStartMinute() { return defaultStartMinute; }
    public void setDefaultStartMinute(int v) { this.defaultStartMinute = v; }
}
