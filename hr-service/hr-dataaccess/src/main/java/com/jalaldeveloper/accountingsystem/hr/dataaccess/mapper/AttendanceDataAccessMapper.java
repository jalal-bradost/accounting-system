package com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.AttendanceEntity;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Attendance;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.AttendanceId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import org.springframework.stereotype.Component;

@Component
public class AttendanceDataAccessMapper {

    public Attendance entityToDomain(AttendanceEntity e) {
        if (e == null) return null;
        return Attendance.builder()
                .id(new AttendanceId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .employeeId(new EmployeeId(e.getEmployeeId()))
                .checkIn(e.getCheckIn())
                .checkOut(e.getCheckOut())
                .checkInMode(e.getCheckInMode())
                .checkOutMode(e.getCheckOutMode())
                .extraHoursMinutes(e.getExtraHoursMinutes())
                .build();
    }

    public AttendanceEntity domainToEntity(Attendance domain, AttendanceEntity existing) {
        AttendanceEntity entity = existing != null ? existing : new AttendanceEntity();
        entity.setId(domain.getId().getId());
        entity.setCompanyId(domain.getCompanyId().getId());
        entity.setEmployeeId(domain.getEmployeeId().getId());
        entity.setCheckIn(domain.getCheckIn());
        entity.setCheckOut(domain.getCheckOut());
        entity.setCheckInMode(domain.getCheckInMode());
        entity.setCheckOutMode(domain.getCheckOutMode());
        entity.setExtraHoursMinutes(domain.getExtraHoursMinutes());
        return entity;
    }
}
