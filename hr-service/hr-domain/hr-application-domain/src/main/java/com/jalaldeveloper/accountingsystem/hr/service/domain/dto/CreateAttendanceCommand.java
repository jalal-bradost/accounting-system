package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class CreateAttendanceCommand {

    private UUID companyId;

    @NotNull
    private UUID employeeId;

    @NotNull
    private Instant checkIn;

    private Instant checkOut;

    private String checkInMode = "manual";

    private String checkOutMode;

    private Integer extraHoursMinutes = 0;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public Instant getCheckIn() { return checkIn; }
    public void setCheckIn(Instant checkIn) { this.checkIn = checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public void setCheckOut(Instant checkOut) { this.checkOut = checkOut; }
    public String getCheckInMode() { return checkInMode; }
    public void setCheckInMode(String checkInMode) { this.checkInMode = checkInMode; }
    public String getCheckOutMode() { return checkOutMode; }
    public void setCheckOutMode(String checkOutMode) { this.checkOutMode = checkOutMode; }
    public Integer getExtraHoursMinutes() { return extraHoursMinutes; }
    public void setExtraHoursMinutes(Integer extraHoursMinutes) { this.extraHoursMinutes = extraHoursMinutes; }
}
