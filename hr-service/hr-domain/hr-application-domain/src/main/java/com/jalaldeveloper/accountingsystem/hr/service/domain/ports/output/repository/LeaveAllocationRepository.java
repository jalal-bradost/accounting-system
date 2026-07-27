package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.LeaveAllocation;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.LeaveAllocationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveAllocationRepository {
    LeaveAllocation save(LeaveAllocation allocation);
    Optional<LeaveAllocation> findById(LeaveAllocationId id);
    List<LeaveAllocation> search(CompanyId companyId, UUID employeeId);
    List<LeaveAllocation> findApproved(CompanyId companyId, UUID employeeId);
}
