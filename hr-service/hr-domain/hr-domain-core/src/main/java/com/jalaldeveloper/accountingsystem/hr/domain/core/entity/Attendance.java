package com.jalaldeveloper.accountingsystem.hr.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.AttendanceId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;

import java.time.Instant;

public class Attendance {

    private final AttendanceId id;
    private final CompanyId companyId;
    private final EmployeeId employeeId;
    private final Instant checkIn;
    private final Instant checkOut;
    private final String checkInMode;
    private final String checkOutMode;
    private final int extraHoursMinutes;

    private Attendance(Builder b) {
        this.id = b.id;
        this.companyId = b.companyId;
        this.employeeId = b.employeeId;
        this.checkIn = b.checkIn;
        this.checkOut = b.checkOut;
        this.checkInMode = b.checkInMode;
        this.checkOutMode = b.checkOutMode;
        this.extraHoursMinutes = b.extraHoursMinutes;
    }

    public void validate() {
        if (companyId == null) throw new HrDomainException("companyId required");
        if (employeeId == null) throw new HrDomainException("employeeId required");
        if (checkIn == null) throw new HrDomainException("checkIn required");
        if (checkOut != null && checkOut.isBefore(checkIn)) {
            throw new HrDomainException("checkOut must be after checkIn");
        }
        if (checkInMode == null || checkInMode.isBlank()) {
            throw new HrDomainException("checkInMode required");
        }
        if (extraHoursMinutes < 0) throw new HrDomainException("extraHoursMinutes must be >= 0");
    }

    public AttendanceId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public EmployeeId getEmployeeId() { return employeeId; }
    public Instant getCheckIn() { return checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public String getCheckInMode() { return checkInMode; }
    public String getCheckOutMode() { return checkOutMode; }
    public int getExtraHoursMinutes() { return extraHoursMinutes; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private AttendanceId id;
        private CompanyId companyId;
        private EmployeeId employeeId;
        private Instant checkIn;
        private Instant checkOut;
        private String checkInMode = "manual";
        private String checkOutMode;
        private int extraHoursMinutes;

        public Builder id(AttendanceId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder employeeId(EmployeeId v) { this.employeeId = v; return this; }
        public Builder checkIn(Instant v) { this.checkIn = v; return this; }
        public Builder checkOut(Instant v) { this.checkOut = v; return this; }
        public Builder checkInMode(String v) { this.checkInMode = v; return this; }
        public Builder checkOutMode(String v) { this.checkOutMode = v; return this; }
        public Builder extraHoursMinutes(int v) { this.extraHoursMinutes = v; return this; }
        public Attendance build() { return new Attendance(this); }
    }
}
