package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.timeoff.TimeOffApi.*;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

public interface TimeOffApplicationService {

    List<TimeOffTypeResponse> listTypes(CompanyId companyId);

    TimeOffTypeResponse createType(SaveTimeOffTypeCommand cmd);

    List<AllocationResponse> listAllocations(CompanyId companyId, UUID employeeId);

    AllocationResponse createAllocation(SaveAllocationCommand cmd);

    AllocationResponse approveAllocation(UUID id);

    AllocationResponse refuseAllocation(UUID id);

    List<LeaveRequestResponse> listRequests(CompanyId companyId, UUID employeeId, String state, int year);

    LeaveRequestResponse createRequest(SaveLeaveRequestCommand cmd);

    LeaveRequestResponse approveRequest(UUID id);

    LeaveRequestResponse refuseRequest(UUID id);

    DashboardResponse dashboard(CompanyId companyId, UUID employeeId, int year);

    TeamSummaryResponse teamSummary(CompanyId companyId, UUID managerId, LocalDate date);
}
