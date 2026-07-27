package com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll;

import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PayrollApi {

    private PayrollApi() {}

    public record WorkingScheduleLineResponse(
            UUID id,
            int dayOfWeek,
            BigDecimal hours,
            int sortOrder
    ) {}

    public record WorkingScheduleSummaryResponse(
            UUID id,
            String name,
            BigDecimal totalHours,
            int lineCount
    ) {}

    public record WorkingScheduleResponse(
            UUID id,
            UUID companyId,
            String name,
            boolean twoWeekCalendar,
            int sortOrder,
            BigDecimal totalHours,
            List<WorkingScheduleLineResponse> lines
    ) {}

    public record WorkingScheduleLineCommand(
            @NotNull int dayOfWeek,
            @NotNull BigDecimal hours,
            Integer sortOrder
    ) {}

    public record SaveWorkingScheduleCommand(
            UUID companyId,
            @NotBlank String name,
            Boolean twoWeekCalendar,
            Integer sortOrder,
            @Valid List<WorkingScheduleLineCommand> lines
    ) {}

    public record EmployeeTypeResponse(
            UUID id,
            UUID companyId,
            String name,
            String countryCode,
            int sortOrder,
            long employeeCount
    ) {}

    public record SaveEmployeeTypeCommand(
            UUID companyId,
            @NotBlank String name,
            String countryCode,
            Integer sortOrder
    ) {}

    public record StructureTypeResponse(
            UUID id,
            UUID companyId,
            String name,
            String scheduledPay,
            String wageType,
            UUID workingScheduleId,
            String workingScheduleName,
            String countryCode,
            UUID payStructureId,
            String payStructureName,
            int sortOrder
    ) {}

    public record SaveStructureTypeCommand(
            UUID companyId,
            @NotBlank String name,
            String scheduledPay,
            String wageType,
            UUID workingScheduleId,
            String countryCode,
            UUID payStructureId,
            Integer sortOrder
    ) {}

    public record SalaryRuleResponse(
            UUID id,
            String name,
            String code,
            String category,
            String amountType,
            BigDecimal amount,
            int sequence,
            boolean active,
            UUID accountId
    ) {}

    public record SalaryRuleCommand(
            UUID id,
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String category,
            String amountType,
            BigDecimal amount,
            Integer sequence,
            Boolean active,
            UUID accountId
    ) {}

    public record StructureSummaryResponse(
            UUID id,
            String name,
            String structureTypeName,
            String scheduledPay,
            String countryCode,
            int ruleCount
    ) {}

    public record StructureResponse(
            UUID id,
            UUID companyId,
            String name,
            UUID structureTypeId,
            String structureTypeName,
            String scheduledPay,
            boolean useWorkedDayLines,
            String countryCode,
            int sortOrder,
            List<SalaryRuleResponse> rules
    ) {}

    public record SaveStructureCommand(
            UUID companyId,
            @NotBlank String name,
            UUID structureTypeId,
            String scheduledPay,
            Boolean useWorkedDayLines,
            String countryCode,
            Integer sortOrder,
            @Valid List<SalaryRuleCommand> rules
    ) {}

    public record ContractSummaryResponse(
            UUID id,
            String name,
            UUID employeeId,
            String employeeName,
            String structureName,
            String workingScheduleName,
            BigDecimal wage,
            String wageType,
            String currencyCode,
            LocalDate dateStart,
            LocalDate dateEnd,
            String state,
            boolean attendanceBased
    ) {}

    public record ContractResponse(
            UUID id,
            UUID companyId,
            UUID employeeId,
            String employeeName,
            String name,
            UUID employeeTypeId,
            String employeeTypeName,
            UUID structureId,
            String structureName,
            UUID workingScheduleId,
            String workingScheduleName,
            BigDecimal wage,
            String wageType,
            String currencyCode,
            LocalDate dateStart,
            LocalDate dateEnd,
            String state,
            boolean attendanceBased
    ) {}

    public record SaveContractCommand(
            UUID companyId,
            UUID employeeId,
            String name,
            UUID employeeTypeId,
            UUID structureId,
            UUID workingScheduleId,
            BigDecimal wage,
            String wageType,
            String currencyCode,
            LocalDate dateStart,
            LocalDate dateEnd,
            String state,
            Boolean attendanceBased
    ) {
        public void validateForSave() {
            if (employeeId == null) {
                throw new HrDomainException("employeeId required");
            }
            if (name == null || name.isBlank()) {
                throw new HrDomainException("name required");
            }
            if (structureId == null) {
                throw new HrDomainException("structureId required");
            }
            if (workingScheduleId == null) {
                throw new HrDomainException("workingScheduleId required");
            }
            if (wage == null) {
                throw new HrDomainException("wage required");
            }
            if (dateStart == null) {
                throw new HrDomainException("dateStart required");
            }
        }
    }

    public record PayslipLineResponse(
            String code,
            String name,
            String category,
            BigDecimal amount
    ) {}

    public record PayslipPreviewResponse(
            UUID contractId,
            String employeeName,
            String currencyCode,
            BigDecimal basicWage,
            BigDecimal totalAllowances,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            List<PayslipLineResponse> lines
    ) {}

    public record CreatePayRunCommand(
            UUID companyId,
            @NotBlank String name,
            @NotNull LocalDate periodStart,
            @NotNull LocalDate periodEnd
    ) {}

    public record PayRunSummaryResponse(
            UUID id,
            String name,
            LocalDate periodStart,
            LocalDate periodEnd,
            String state,
            int payslipCount,
            BigDecimal totalNet
    ) {}

    public record PayslipLineDetailResponse(
            UUID id,
            String code,
            String name,
            String category,
            BigDecimal amount,
            UUID accountId
    ) {}

    public record PayslipResponse(
            UUID id,
            UUID payRunId,
            UUID employeeId,
            String employeeName,
            UUID contractId,
            String currencyCode,
            String state,
            BigDecimal basic,
            BigDecimal allowances,
            BigDecimal deductions,
            BigDecimal net,
            BigDecimal workedDays,
            BigDecimal absenceDays,
            List<PayslipLineDetailResponse> lines
    ) {}

    public record PayRunResponse(
            UUID id,
            UUID companyId,
            String name,
            LocalDate periodStart,
            LocalDate periodEnd,
            String state,
            UUID journalEntryId,
            UUID paymentJournalEntryId,
            List<PayslipResponse> payslips
    ) {}

    public record PayRunCommand(
            @NotNull UUID bankJournalId,
            LocalDate paymentDate,
            String reference
    ) {}
}
