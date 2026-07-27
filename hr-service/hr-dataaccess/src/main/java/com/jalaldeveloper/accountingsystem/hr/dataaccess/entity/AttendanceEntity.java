package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hr_attendance")
public class AttendanceEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "check_in", nullable = false)
    private Instant checkIn;

    @Column(name = "check_out")
    private Instant checkOut;

    @Column(name = "check_in_mode", nullable = false, length = 32)
    private String checkInMode;

    @Column(name = "check_out_mode", length = 32)
    private String checkOutMode;

    @Column(name = "extra_hours_minutes", nullable = false)
    private int extraHoursMinutes;

    public AttendanceEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public int getExtraHoursMinutes() { return extraHoursMinutes; }
    public void setExtraHoursMinutes(int extraHoursMinutes) { this.extraHoursMinutes = extraHoursMinutes; }
}
