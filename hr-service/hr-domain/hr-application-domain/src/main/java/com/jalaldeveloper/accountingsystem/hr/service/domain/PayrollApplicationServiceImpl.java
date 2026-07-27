package com.jalaldeveloper.accountingsystem.hr.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.service.PayrollDomainService;
import com.jalaldeveloper.accountingsystem.hr.domain.core.service.PayrollDomainService.PayslipComputationInput;
import com.jalaldeveloper.accountingsystem.hr.domain.core.service.PayrollDomainService.SalaryRuleInput;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll.PayrollApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.PayrollApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.accounting.CompanyCurrencyPort;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence.*;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class PayrollApplicationServiceImpl implements PayrollApplicationService {

    private static final String DEFAULT_SCHEDULED_PAY = "month";
    private static final String DEFAULT_WAGE_TYPE = "fixed";
    private static final String DEFAULT_CONTRACT_STATE = "draft";

    private final PayrollPersistence persistence;
    private final CompanyCurrencyPort companyCurrencyPort;
    private final PayrollDomainService payrollDomainService = new PayrollDomainService();
    private final ObjectProvider<CompanyContext> companyContextProvider;

    PayrollApplicationServiceImpl(PayrollPersistence persistence,
                                  CompanyCurrencyPort companyCurrencyPort,
                                  ObjectProvider<CompanyContext> companyContextProvider) {
        this.persistence = persistence;
        this.companyCurrencyPort = companyCurrencyPort;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkingScheduleSummaryResponse> listWorkingSchedules(CompanyId companyId) {
        return persistence.listSchedules(companyId.getId()).stream()
                .map(s -> new WorkingScheduleSummaryResponse(
                        s.id(),
                        s.name(),
                        totalHours(s.lines()),
                        s.lines().size()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkingScheduleResponse getWorkingSchedule(UUID id) {
        ScheduleRow row = loadSchedule(id);
        return toScheduleResponse(row);
    }

    @Override
    @Transactional
    public WorkingScheduleResponse createWorkingSchedule(SaveWorkingScheduleCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        ScheduleRow saved = persistence.saveSchedule(toScheduleRow(null, companyId, cmd));
        return toScheduleResponse(saved);
    }

    @Override
    @Transactional
    public WorkingScheduleResponse updateWorkingSchedule(UUID id, SaveWorkingScheduleCommand cmd) {
        ScheduleRow existing = loadSchedule(id);
        CompanyId companyId = resolveCompany(cmd.companyId() != null ? cmd.companyId() : existing.companyId());
        ScheduleRow saved = persistence.saveSchedule(toScheduleRow(id, companyId, cmd));
        return toScheduleResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeTypeResponse> listEmployeeTypes(CompanyId companyId) {
        return persistence.listEmployeeTypes(companyId.getId()).stream()
                .map(row -> new EmployeeTypeResponse(
                        row.id(),
                        row.companyId(),
                        row.name(),
                        row.countryCode(),
                        row.sortOrder(),
                        persistence.countContractsByEmployeeType(companyId.getId(), row.id())))
                .toList();
    }

    @Override
    @Transactional
    public EmployeeTypeResponse createEmployeeType(SaveEmployeeTypeCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        EmployeeTypeRow saved = persistence.saveEmployeeType(toEmployeeTypeRow(null, companyId, cmd));
        return toEmployeeTypeResponse(saved, companyId.getId());
    }

    @Override
    @Transactional
    public EmployeeTypeResponse updateEmployeeType(UUID id, SaveEmployeeTypeCommand cmd) {
        EmployeeTypeRow existing = loadEmployeeType(id);
        CompanyId companyId = resolveCompany(cmd.companyId() != null ? cmd.companyId() : existing.companyId());
        EmployeeTypeRow saved = persistence.saveEmployeeType(toEmployeeTypeRow(id, companyId, cmd));
        return toEmployeeTypeResponse(saved, companyId.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructureTypeResponse> listStructureTypes(CompanyId companyId) {
        return persistence.listStructureTypes(companyId.getId()).stream()
                .map(this::toStructureTypeResponse)
                .toList();
    }

    @Override
    @Transactional
    public StructureTypeResponse createStructureType(SaveStructureTypeCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        StructureTypeRow saved = persistence.saveStructureType(toStructureTypeRow(null, companyId, cmd));
        return toStructureTypeResponse(saved);
    }

    @Override
    @Transactional
    public StructureTypeResponse updateStructureType(UUID id, SaveStructureTypeCommand cmd) {
        StructureTypeRow existing = loadStructureType(id);
        CompanyId companyId = resolveCompany(cmd.companyId() != null ? cmd.companyId() : existing.companyId());
        StructureTypeRow saved = persistence.saveStructureType(toStructureTypeRow(id, companyId, cmd));
        return toStructureTypeResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructureSummaryResponse> listStructures(CompanyId companyId) {
        return persistence.listStructures(companyId.getId()).stream()
                .map(row -> new StructureSummaryResponse(
                        row.id(),
                        row.name(),
                        persistence.findStructureTypeName(row.structureTypeId()).orElse(null),
                        row.scheduledPay(),
                        row.countryCode(),
                        row.rules().size()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StructureResponse getStructure(UUID id) {
        return toStructureResponse(loadStructure(id));
    }

    @Override
    @Transactional
    public StructureResponse createStructure(SaveStructureCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        StructureRow saved = persistence.saveStructure(toStructureRow(null, companyId, cmd));
        return toStructureResponse(saved);
    }

    @Override
    @Transactional
    public StructureResponse updateStructure(UUID id, SaveStructureCommand cmd) {
        StructureRow existing = loadStructure(id);
        CompanyId companyId = resolveCompany(cmd.companyId() != null ? cmd.companyId() : existing.companyId());
        StructureRow saved = persistence.saveStructure(toStructureRow(id, companyId, cmd));
        return toStructureResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> listContracts(CompanyId companyId, UUID employeeId, Pageable pageable) {
        List<ContractRow> rows = persistence.listContracts(
                companyId.getId(), employeeId, pageable.getPageNumber(), pageable.getPageSize());
        long total = persistence.countContracts(companyId.getId(), employeeId);
        List<ContractSummaryResponse> content = rows.stream()
                .map(row -> toContractSummary(row, persistence.findEmployeeName(row.employeeId()).orElse(null)))
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContract(UUID id) {
        return toContractResponse(loadContract(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getActiveContractForEmployee(CompanyId companyId, UUID employeeId) {
        ContractRow row = persistence.findRunningContract(companyId.getId(), employeeId)
                .orElse(null);
        if (row == null) {
            return null;
        }
        return toContractResponse(row);
    }

    @Override
    @Transactional
    public ContractResponse createContract(SaveContractCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        cmd.validateForSave();
        validateContractReferences(companyId, cmd);
        ContractRow saved = persistence.saveContract(toContractRow(null, companyId, cmd));
        return toContractResponse(saved);
    }

    @Override
    @Transactional
    public ContractResponse updateContract(UUID id, SaveContractCommand cmd) {
        ContractRow existing = loadContract(id);
        CompanyId companyId = resolveCompany(cmd.companyId() != null ? cmd.companyId() : existing.companyId());
        SaveContractCommand merged = mergeContractUpdate(existing, cmd);
        merged.validateForSave();
        validateContractReferences(companyId, merged);
        ContractRow saved = persistence.saveContract(toContractRow(id, companyId, merged));
        return toContractResponse(saved);
    }

    private SaveContractCommand mergeContractUpdate(ContractRow existing, SaveContractCommand cmd) {
        return new SaveContractCommand(
                cmd.companyId() != null ? cmd.companyId() : existing.companyId(),
                cmd.employeeId() != null ? cmd.employeeId() : existing.employeeId(),
                cmd.name() != null && !cmd.name().isBlank() ? cmd.name() : existing.name(),
                cmd.employeeTypeId(),
                cmd.structureId() != null ? cmd.structureId() : existing.structureId(),
                cmd.workingScheduleId() != null ? cmd.workingScheduleId() : existing.workingScheduleId(),
                cmd.wage() != null ? cmd.wage() : existing.wage(),
                cmd.wageType() != null ? cmd.wageType() : existing.wageType(),
                cmd.currencyCode() != null && !cmd.currencyCode().isBlank()
                        ? cmd.currencyCode()
                        : existing.currencyCode(),
                cmd.dateStart() != null ? cmd.dateStart() : existing.dateStart(),
                cmd.dateEnd(),
                cmd.state() != null ? cmd.state() : existing.state(),
                cmd.attendanceBased() != null ? cmd.attendanceBased() : existing.attendanceBased());
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipPreviewResponse previewPayslip(UUID contractId) {
        ContractRow contract = loadContract(contractId);
        StructureRow structure = loadStructure(contract.structureId());
        BigDecimal expectedDays = persistence.countExpectedWorkDays(
                contract.workingScheduleId(), contract.dateStart(), contract.dateEnd() != null
                        ? contract.dateEnd() : contract.dateStart());
        BigDecimal workedDays = persistence.countWorkedDays(
                contract.employeeId(), contract.dateStart(),
                contract.dateEnd() != null ? contract.dateEnd() : contract.dateStart());
        BigDecimal absenceDays = expectedDays.subtract(workedDays).max(BigDecimal.ZERO);

        List<SalaryRuleInput> rules = structure.rules().stream()
                .map(r -> new SalaryRuleInput(
                        r.id(), r.code(), r.name(), r.category(), r.amountType(),
                        r.amount(), r.active(), r.accountId()))
                .toList();

        PayslipComputationInput input = new PayslipComputationInput(
                null,
                contract.employeeId(),
                contract.id(),
                contract.currencyCode(),
                contract.wage(),
                contract.attendanceBased(),
                expectedDays,
                workedDays,
                absenceDays,
                rules);

        var payslip = payrollDomainService.computePayslip(input);
        List<PayslipLineResponse> lines = payslip.getLines().stream()
                .map(l -> new PayslipLineResponse(l.getCode(), l.getName(), l.getCategory(), l.getAmount()))
                .toList();

        return new PayslipPreviewResponse(
                contract.id(),
                persistence.findEmployeeName(contract.employeeId()).orElse(null),
                contract.currencyCode(),
                payslip.getBasic(),
                payslip.getAllowances(),
                payslip.getDeductions(),
                payslip.getNet(),
                lines);
    }

    private void validateContractReferences(CompanyId companyId, SaveContractCommand cmd) {
        if (!persistence.employeeExists(cmd.employeeId())) {
            throw new HrDomainException("Employee not found: " + cmd.employeeId());
        }
        loadStructure(cmd.structureId());
        loadSchedule(cmd.workingScheduleId());
        if (cmd.employeeTypeId() != null) {
            loadEmployeeType(cmd.employeeTypeId());
        }
        String currency = cmd.currencyCode() != null
                ? cmd.currencyCode()
                : companyCurrencyPort.defaultCurrencyCode(companyId);
        if (!companyCurrencyPort.isActiveCurrency(companyId, currency)) {
            throw new HrDomainException("Currency is not active for company: " + currency);
        }
    }

    private ScheduleRow loadSchedule(UUID id) {
        return persistence.findSchedule(id)
                .orElseThrow(() -> new HrDomainException("Working schedule not found: " + id));
    }

    private EmployeeTypeRow loadEmployeeType(UUID id) {
        return persistence.findEmployeeType(id)
                .orElseThrow(() -> new HrDomainException("Employee type not found: " + id));
    }

    private StructureTypeRow loadStructureType(UUID id) {
        return persistence.findStructureType(id)
                .orElseThrow(() -> new HrDomainException("Structure type not found: " + id));
    }

    private StructureRow loadStructure(UUID id) {
        return persistence.findStructure(id)
                .orElseThrow(() -> new HrDomainException("Pay structure not found: " + id));
    }

    private ContractRow loadContract(UUID id) {
        return persistence.findContract(id)
                .orElseThrow(() -> new HrDomainException("Contract not found: " + id));
    }

    private CompanyId resolveCompany(UUID companyId) {
        if (companyId != null) {
            return new CompanyId(companyId);
        }
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.requireCompany();
        }
        throw new HrDomainException("companyId required");
    }

    private ScheduleRow toScheduleRow(UUID id, CompanyId companyId, SaveWorkingScheduleCommand cmd) {
        List<ScheduleLineRow> lines = new ArrayList<>();
        if (cmd.lines() != null) {
            int i = 0;
            for (WorkingScheduleLineCommand line : cmd.lines()) {
                lines.add(new ScheduleLineRow(
                        null,
                        (short) line.dayOfWeek(),
                        line.hours(),
                        line.sortOrder() != null ? line.sortOrder() : i++));
            }
        }
        return new ScheduleRow(
                id,
                companyId.getId(),
                cmd.name(),
                Boolean.TRUE.equals(cmd.twoWeekCalendar()),
                cmd.sortOrder() != null ? cmd.sortOrder() : 0,
                lines);
    }

    private WorkingScheduleResponse toScheduleResponse(ScheduleRow row) {
        List<WorkingScheduleLineResponse> lines = row.lines().stream()
                .map(l -> new WorkingScheduleLineResponse(l.id(), l.dayOfWeek(), l.hours(), l.sortOrder()))
                .toList();
        return new WorkingScheduleResponse(
                row.id(),
                row.companyId(),
                row.name(),
                row.twoWeekCalendar(),
                row.sortOrder(),
                totalHours(row.lines()),
                lines);
    }

    private EmployeeTypeRow toEmployeeTypeRow(UUID id, CompanyId companyId, SaveEmployeeTypeCommand cmd) {
        return new EmployeeTypeRow(
                id,
                companyId.getId(),
                cmd.name(),
                cmd.countryCode(),
                cmd.sortOrder() != null ? cmd.sortOrder() : 0);
    }

    private EmployeeTypeResponse toEmployeeTypeResponse(EmployeeTypeRow row, UUID companyId) {
        return new EmployeeTypeResponse(
                row.id(),
                row.companyId(),
                row.name(),
                row.countryCode(),
                row.sortOrder(),
                persistence.countContractsByEmployeeType(companyId, row.id()));
    }

    private StructureTypeRow toStructureTypeRow(UUID id, CompanyId companyId, SaveStructureTypeCommand cmd) {
        return new StructureTypeRow(
                id,
                companyId.getId(),
                cmd.name(),
                cmd.scheduledPay() != null ? cmd.scheduledPay() : DEFAULT_SCHEDULED_PAY,
                cmd.wageType() != null ? cmd.wageType() : DEFAULT_WAGE_TYPE,
                cmd.workingScheduleId(),
                cmd.countryCode(),
                cmd.payStructureId(),
                cmd.sortOrder() != null ? cmd.sortOrder() : 0);
    }

    private StructureTypeResponse toStructureTypeResponse(StructureTypeRow row) {
        return new StructureTypeResponse(
                row.id(),
                row.companyId(),
                row.name(),
                row.scheduledPay(),
                row.wageType(),
                row.workingScheduleId(),
                persistence.findScheduleName(row.workingScheduleId()).orElse(null),
                row.countryCode(),
                row.payStructureId(),
                persistence.findStructureName(row.payStructureId()).orElse(null),
                row.sortOrder());
    }

    private StructureRow toStructureRow(UUID id, CompanyId companyId, SaveStructureCommand cmd) {
        List<SalaryRuleRow> rules = new ArrayList<>();
        if (cmd.rules() != null) {
            int seq = 10;
            for (SalaryRuleCommand rule : cmd.rules()) {
                rules.add(new SalaryRuleRow(
                        rule.id(),
                        rule.name(),
                        rule.code(),
                        rule.category(),
                        rule.amountType() != null ? rule.amountType() : "fixed",
                        rule.amount() != null ? rule.amount() : BigDecimal.ZERO,
                        rule.sequence() != null ? rule.sequence() : seq,
                        rule.active() == null || rule.active(),
                        rule.accountId()));
                seq += 10;
            }
        }
        return new StructureRow(
                id,
                companyId.getId(),
                cmd.name(),
                cmd.structureTypeId(),
                cmd.scheduledPay() != null ? cmd.scheduledPay() : DEFAULT_SCHEDULED_PAY,
                cmd.useWorkedDayLines() == null || cmd.useWorkedDayLines(),
                cmd.countryCode(),
                cmd.sortOrder() != null ? cmd.sortOrder() : 0,
                rules);
    }

    private StructureResponse toStructureResponse(StructureRow row) {
        List<SalaryRuleResponse> rules = row.rules().stream()
                .map(r -> new SalaryRuleResponse(
                        r.id(), r.name(), r.code(), r.category(), r.amountType(), r.amount(), r.sequence(), r.active(), r.accountId()))
                .toList();
        return new StructureResponse(
                row.id(),
                row.companyId(),
                row.name(),
                row.structureTypeId(),
                persistence.findStructureTypeName(row.structureTypeId()).orElse(null),
                row.scheduledPay(),
                row.useWorkedDayLines(),
                row.countryCode(),
                row.sortOrder(),
                rules);
    }

    private ContractRow toContractRow(UUID id, CompanyId companyId, SaveContractCommand cmd) {
        String currency = cmd.currencyCode() != null
                ? cmd.currencyCode()
                : companyCurrencyPort.defaultCurrencyCode(companyId);
        return new ContractRow(
                id,
                companyId.getId(),
                cmd.employeeId(),
                cmd.name(),
                cmd.employeeTypeId(),
                cmd.structureId(),
                cmd.workingScheduleId(),
                cmd.wage(),
                cmd.wageType() != null ? cmd.wageType() : DEFAULT_WAGE_TYPE,
                currency,
                cmd.dateStart(),
                cmd.dateEnd(),
                cmd.state() != null ? cmd.state() : DEFAULT_CONTRACT_STATE,
                cmd.attendanceBased() == null || cmd.attendanceBased());
    }

    private ContractSummaryResponse toContractSummary(ContractRow row, String employeeName) {
        return new ContractSummaryResponse(
                row.id(),
                row.name(),
                row.employeeId(),
                employeeName,
                persistence.findStructureName(row.structureId()).orElse(null),
                persistence.findScheduleName(row.workingScheduleId()).orElse(null),
                row.wage(),
                row.wageType(),
                row.currencyCode(),
                row.dateStart(),
                row.dateEnd(),
                row.state(),
                row.attendanceBased());
    }

    private ContractResponse toContractResponse(ContractRow row) {
        return new ContractResponse(
                row.id(),
                row.companyId(),
                row.employeeId(),
                persistence.findEmployeeName(row.employeeId()).orElse(null),
                row.name(),
                row.employeeTypeId(),
                persistence.findEmployeeTypeName(row.employeeTypeId()).orElse(null),
                row.structureId(),
                persistence.findStructureName(row.structureId()).orElse(null),
                row.workingScheduleId(),
                persistence.findScheduleName(row.workingScheduleId()).orElse(null),
                row.wage(),
                row.wageType(),
                row.currencyCode(),
                row.dateStart(),
                row.dateEnd(),
                row.state(),
                row.attendanceBased());
    }

    private static BigDecimal totalHours(List<ScheduleLineRow> lines) {
        return lines.stream().map(ScheduleLineRow::hours).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
