package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollPersistence {

    record ScheduleLineRow(UUID id, short dayOfWeek, BigDecimal hours, int sortOrder) {}

    record ScheduleRow(UUID id, UUID companyId, String name, boolean twoWeekCalendar, int sortOrder,
                       List<ScheduleLineRow> lines) {}

    record EmployeeTypeRow(UUID id, UUID companyId, String name, String countryCode, int sortOrder) {}

    record StructureTypeRow(UUID id, UUID companyId, String name, String scheduledPay, String wageType,
                            UUID workingScheduleId, String countryCode, UUID payStructureId, int sortOrder) {}

    record SalaryRuleRow(UUID id, String name, String code, String category, String amountType,
                         BigDecimal amount, int sequence, boolean active, UUID accountId) {}

    record StructureRow(UUID id, UUID companyId, String name, UUID structureTypeId, String scheduledPay,
                        boolean useWorkedDayLines, String countryCode, int sortOrder, List<SalaryRuleRow> rules) {}

    record ContractRow(UUID id, UUID companyId, UUID employeeId, String name, UUID employeeTypeId,
                       UUID structureId, UUID workingScheduleId, BigDecimal wage, String wageType,
                       String currencyCode, LocalDate dateStart, LocalDate dateEnd, String state,
                       boolean attendanceBased) {}

    List<ScheduleRow> listSchedules(UUID companyId);

    Optional<ScheduleRow> findSchedule(UUID id);

    ScheduleRow saveSchedule(ScheduleRow row);

    List<EmployeeTypeRow> listEmployeeTypes(UUID companyId);

    Optional<EmployeeTypeRow> findEmployeeType(UUID id);

    EmployeeTypeRow saveEmployeeType(EmployeeTypeRow row);

    long countContractsByEmployeeType(UUID companyId, UUID employeeTypeId);

    List<StructureTypeRow> listStructureTypes(UUID companyId);

    Optional<StructureTypeRow> findStructureType(UUID id);

    StructureTypeRow saveStructureType(StructureTypeRow row);

    List<StructureRow> listStructures(UUID companyId);

    Optional<StructureRow> findStructure(UUID id);

    StructureRow saveStructure(StructureRow row);

    List<ContractRow> listContracts(UUID companyId, UUID employeeId, int page, int size);

    long countContracts(UUID companyId, UUID employeeId);

    Optional<ContractRow> findContract(UUID id);

    Optional<ContractRow> findRunningContract(UUID companyId, UUID employeeId);

    ContractRow saveContract(ContractRow row);

    Optional<String> findEmployeeName(UUID employeeId);

    Optional<String> findScheduleName(UUID scheduleId);

    Optional<String> findStructureName(UUID structureId);

    Optional<String> findEmployeeTypeName(UUID employeeTypeId);

    Optional<String> findStructureTypeName(UUID structureTypeId);

    boolean employeeExists(UUID employeeId);

    List<ContractRow> listRunningContracts(UUID companyId, LocalDate periodStart, LocalDate periodEnd);

    BigDecimal countWorkedDays(UUID employeeId, LocalDate start, LocalDate end);

    BigDecimal countExpectedWorkDays(UUID scheduleId, LocalDate start, LocalDate end);
}
