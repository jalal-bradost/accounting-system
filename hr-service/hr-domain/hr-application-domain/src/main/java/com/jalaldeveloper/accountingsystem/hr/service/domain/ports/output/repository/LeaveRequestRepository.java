package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.LeaveRequest;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.LeaveRequestId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRequestRepository {
    LeaveRequest save(LeaveRequest request);
    Optional<LeaveRequest> findById(LeaveRequestId id);
    List<LeaveRequest> search(CompanyId companyId, UUID employeeId, String state, LocalDate from, LocalDate to);
    BigDecimal sumValidatedDays(CompanyId companyId, UUID employeeId, UUID typeId, LocalDate from, LocalDate to);
    long countPending(CompanyId companyId, UUID employeeId);
}
