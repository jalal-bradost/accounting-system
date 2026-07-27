package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public class UpdateAttendanceCommand {

    private UUID employeeId;
    private Instant checkIn;
    private Instant checkOut;
    private Boolean checkOutReset;
    private String checkInMode;
    private String checkOutMode;
    private Integer extraHoursMinutes;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public Instant getCheckIn() { return checkIn; }
    public void setCheckIn(Instant checkIn) { this.checkIn = checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public void setCheckOut(Instant checkOut) { this.checkOut = checkOut; }
    public Boolean getCheckOutReset() { return checkOutReset; }
    public void setCheckOutReset(Boolean checkOutReset) { this.checkOutReset = checkOutReset; }
    public String getCheckInMode() { return checkInMode; }
    public void setCheckInMode(String checkInMode) { this.checkInMode = checkInMode; }
    public String getCheckOutMode() { return checkOutMode; }
    public void setCheckOutMode(String checkOutMode) { this.checkOutMode = checkOutMode; }
    public Integer getExtraHoursMinutes() { return extraHoursMinutes; }
    public void setExtraHoursMinutes(Integer extraHoursMinutes) { this.extraHoursMinutes = extraHoursMinutes; }
}
