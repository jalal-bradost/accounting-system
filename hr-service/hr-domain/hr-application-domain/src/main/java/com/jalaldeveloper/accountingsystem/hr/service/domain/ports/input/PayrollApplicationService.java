package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll.PayrollApi.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PayrollApplicationService {

    List<WorkingScheduleSummaryResponse> listWorkingSchedules(CompanyId companyId);

    WorkingScheduleResponse getWorkingSchedule(UUID id);

    WorkingScheduleResponse createWorkingSchedule(SaveWorkingScheduleCommand cmd);

    WorkingScheduleResponse updateWorkingSchedule(UUID id, SaveWorkingScheduleCommand cmd);

    List<EmployeeTypeResponse> listEmployeeTypes(CompanyId companyId);

    EmployeeTypeResponse createEmployeeType(SaveEmployeeTypeCommand cmd);

    EmployeeTypeResponse updateEmployeeType(UUID id, SaveEmployeeTypeCommand cmd);

    List<StructureTypeResponse> listStructureTypes(CompanyId companyId);

    StructureTypeResponse createStructureType(SaveStructureTypeCommand cmd);

    StructureTypeResponse updateStructureType(UUID id, SaveStructureTypeCommand cmd);

    List<StructureSummaryResponse> listStructures(CompanyId companyId);

    StructureResponse getStructure(UUID id);

    StructureResponse createStructure(SaveStructureCommand cmd);

    StructureResponse updateStructure(UUID id, SaveStructureCommand cmd);

    Page<ContractSummaryResponse> listContracts(CompanyId companyId, UUID employeeId, Pageable pageable);

    ContractResponse getContract(UUID id);

    ContractResponse getActiveContractForEmployee(CompanyId companyId, UUID employeeId);

    ContractResponse createContract(SaveContractCommand cmd);

    ContractResponse updateContract(UUID id, SaveContractCommand cmd);

    PayslipPreviewResponse previewPayslip(UUID contractId);
}
