package com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.*;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.LeaveAllocation;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.LeaveRequest;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.TimeOffType;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.*;

public final class TimeOffDataAccessMapper {

    private TimeOffDataAccessMapper() {}

    public static TimeOffType entityToDomain(TimeOffTypeEntity e) {
        return TimeOffType.builder()
                .id(new TimeOffTypeId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .name(e.getName())
                .code(e.getCode())
                .displayCode(e.getDisplayCode())
                .countryCode(e.getCountryCode())
                .colorHex(e.getColorHex())
                .sortOrder(e.getSortOrder())
                .active(e.isActive())
                .build();
    }

    public static TimeOffTypeEntity domainToEntity(TimeOffType d, TimeOffTypeEntity existing) {
        TimeOffTypeEntity e = existing != null ? existing : new TimeOffTypeEntity();
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setName(d.getName());
        e.setCode(d.getCode());
        e.setDisplayCode(d.getDisplayCode());
        e.setCountryCode(d.getCountryCode());
        e.setColorHex(d.getColorHex());
        e.setSortOrder(d.getSortOrder());
        e.setActive(d.isActive());
        return e;
    }

    public static LeaveAllocation entityToDomain(LeaveAllocationEntity e) {
        return LeaveAllocation.builder()
                .id(new LeaveAllocationId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .employeeId(new EmployeeId(e.getEmployeeId()))
                .timeOffTypeId(new TimeOffTypeId(e.getTimeOffTypeId()))
                .name(e.getName())
                .numberOfDays(e.getNumberOfDays())
                .allocationType(e.getAllocationType())
                .state(e.getState())
                .dateFrom(e.getDateFrom())
                .dateTo(e.getDateTo())
                .build();
    }

    public static LeaveAllocationEntity domainToEntity(LeaveAllocation d, LeaveAllocationEntity existing) {
        LeaveAllocationEntity e = existing != null ? existing : new LeaveAllocationEntity();
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setEmployeeId(d.getEmployeeId().getId());
        e.setTimeOffTypeId(d.getTimeOffTypeId().getId());
        e.setName(d.getName());
        e.setNumberOfDays(d.getNumberOfDays());
        e.setAllocationType(d.getAllocationType());
        e.setState(d.getState());
        e.setDateFrom(d.getDateFrom());
        e.setDateTo(d.getDateTo());
        return e;
    }

    public static LeaveRequest entityToDomain(LeaveRequestEntity e) {
        return LeaveRequest.builder()
                .id(new LeaveRequestId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .employeeId(new EmployeeId(e.getEmployeeId()))
                .timeOffTypeId(new TimeOffTypeId(e.getTimeOffTypeId()))
                .dateFrom(e.getDateFrom())
                .dateTo(e.getDateTo())
                .numberOfDays(e.getNumberOfDays())
                .state(e.getState())
                .description(e.getDescription())
                .build();
    }

    public static LeaveRequestEntity domainToEntity(LeaveRequest d, LeaveRequestEntity existing) {
        LeaveRequestEntity e = existing != null ? existing : new LeaveRequestEntity();
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setEmployeeId(d.getEmployeeId().getId());
        e.setTimeOffTypeId(d.getTimeOffTypeId().getId());
        e.setDateFrom(d.getDateFrom());
        e.setDateTo(d.getDateTo());
        e.setNumberOfDays(d.getNumberOfDays());
        e.setState(d.getState());
        e.setDescription(d.getDescription());
        return e;
    }
}
