package com.jalaldeveloper.accountingsystem.hr.service.domain;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.CreateJournalEntryResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.JournalItemCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.AccountingReferenceLookupPort;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.PayRun;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Payslip;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.PayslipLine;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.service.PayrollDomainService;
import com.jalaldeveloper.accountingsystem.hr.domain.core.service.PayrollDomainService.PayslipComputationInput;
import com.jalaldeveloper.accountingsystem.hr.domain.core.service.PayrollDomainService.SalaryRuleInput;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll.PayrollApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.PayRunApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.accounting.CompanyCurrencyPort;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayRunRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence.*;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class PayRunApplicationServiceImpl implements PayRunApplicationService {

    private static final String SALARY_EXPENSE = "430016";
    private static final String SALARIES_PAYABLE = "430017";
    private static final String DEDUCTIONS_PAYABLE = "430018";
    private static final String PAY_JOURNAL_CODE = "PAY";

    private final PayRunRepository payRunRepository;
    private final PayrollPersistence payrollPersistence;
    private final PayrollDomainService payrollDomainService = new PayrollDomainService();
    private final CompanyCurrencyPort companyCurrencyPort;
    private final JournalEntryApplicationService journalEntryApplicationService;
    private final AccountingReferenceLookupPort accountingReferenceLookupPort;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    PayRunApplicationServiceImpl(PayRunRepository payRunRepository,
                                 PayrollPersistence payrollPersistence,
                                 CompanyCurrencyPort companyCurrencyPort,
                                 JournalEntryApplicationService journalEntryApplicationService,
                                 AccountingReferenceLookupPort accountingReferenceLookupPort,
                                 ObjectProvider<CompanyContext> companyContextProvider) {
        this.payRunRepository = payRunRepository;
        this.payrollPersistence = payrollPersistence;
        this.companyCurrencyPort = companyCurrencyPort;
        this.journalEntryApplicationService = journalEntryApplicationService;
        this.accountingReferenceLookupPort = accountingReferenceLookupPort;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public PayRunResponse createRun(CreatePayRunCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        PayRun payRun = PayRun.builder()
                .companyId(companyId)
                .name(cmd.name())
                .periodStart(cmd.periodStart())
                .periodEnd(cmd.periodEnd())
                .state(PayRun.STATE_DRAFT)
                .build();
        payRun.validate();
        return toResponse(payRunRepository.save(payRun));
    }

    @Override
    @Transactional
    public PayRunResponse computeRun(UUID id) {
        PayRun payRun = loadRun(id);
        if (!PayRun.STATE_DRAFT.equals(payRun.getState()) && !PayRun.STATE_COMPUTED.equals(payRun.getState())) {
            throw new HrDomainException("Pay run cannot be computed in state: " + payRun.getState());
        }
        List<ContractRow> contracts = payrollPersistence.listRunningContracts(
                payRun.getCompanyId().getId(), payRun.getPeriodStart(), payRun.getPeriodEnd());
        List<Payslip> payslips = new ArrayList<>();
        for (ContractRow contract : contracts) {
            payslips.add(computePayslipForContract(payRun, contract));
        }
        PayRun computed = payRun.withPayslips(payslips);
        return toResponse(payRunRepository.save(computed));
    }

    @Override
    @Transactional(readOnly = true)
    public PayRunResponse getRun(UUID id) {
        return toResponse(loadRun(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayRunSummaryResponse> listRuns(CompanyId companyId) {
        return payRunRepository.listByCompany(companyId.getId()).stream()
                .map(run -> new PayRunSummaryResponse(
                        run.getId(),
                        run.getName(),
                        run.getPeriodStart(),
                        run.getPeriodEnd(),
                        run.getState(),
                        run.getPayslips().size(),
                        run.getPayslips().stream().map(Payslip::getNet).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipResponse getPayslip(UUID payslipId) {
        Payslip payslip = payRunRepository.findPayslipById(payslipId)
                .orElseThrow(() -> new HrDomainException("Payslip not found: " + payslipId));
        return toPayslipResponse(payslip);
    }

    @Override
    @Transactional
    public PayRunResponse postRun(UUID id) {
        PayRun payRun = loadRun(id);
        if (!PayRun.STATE_COMPUTED.equals(payRun.getState())) {
            throw new HrDomainException("Only computed pay runs can be posted");
        }
        UUID companyId = payRun.getCompanyId().getId();
        UUID payJournalId = accountingReferenceLookupPort.resolveJournalIdByCode(companyId, PAY_JOURNAL_CODE);
        UUID defaultExpenseAccount = accountingReferenceLookupPort.resolveAccountIdByCode(companyId, SALARY_EXPENSE);
        UUID salariesPayableAccount = accountingReferenceLookupPort.resolveAccountIdByCode(companyId, SALARIES_PAYABLE);
        UUID deductionsPayableAccount = accountingReferenceLookupPort.resolveAccountIdByCode(companyId, DEDUCTIONS_PAYABLE);

        String currencyCode = companyCurrencyPort.defaultCurrencyCode(payRun.getCompanyId());
        List<JournalItemCommand> items = new ArrayList<>();
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (Payslip payslip : payRun.getPayslips()) {
            currencyCode = payslip.getCurrencyCode();
            for (PayslipLine line : payslip.getLines()) {
                if ("net".equalsIgnoreCase(line.getCategory())) {
                    continue;
                }
                if ("deduction".equalsIgnoreCase(line.getCategory()) && line.getAmount().signum() > 0) {
                    totalDeductions = totalDeductions.add(line.getAmount());
                    continue;
                }
                if (line.getAmount().signum() <= 0) {
                    continue;
                }
                UUID accountId = line.getAccountId() != null ? line.getAccountId() : defaultExpenseAccount;
                items.add(new JournalItemCommand(
                        accountId, line.getName(), line.getAmount(), BigDecimal.ZERO,
                        payslip.getCurrencyCode(), line.getAmount(), null));
            }
            totalNet = totalNet.add(payslip.getNet());
        }

        if (totalDeductions.signum() > 0) {
            items.add(new JournalItemCommand(
                    deductionsPayableAccount, "Payroll deductions", BigDecimal.ZERO, totalDeductions,
                    currencyCode, totalDeductions.negate(), null));
        }
        if (totalNet.signum() > 0) {
            items.add(new JournalItemCommand(
                    salariesPayableAccount, "Salaries payable", BigDecimal.ZERO, totalNet,
                    currencyCode, totalNet.negate(), null));
        }

        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                payJournalId,
                "",
                payRun.getPeriodEnd(),
                currencyCode,
                null,
                items);
        CreateJournalEntryResponse created = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(created.getJournalEntryId());

        PayRun posted = payRun.markPosted(created.getJournalEntryId());
        return toResponse(payRunRepository.save(posted));
    }

    @Override
    @Transactional
    public PayRunResponse payRun(UUID id, PayRunCommand cmd) {
        PayRun payRun = loadRun(id);
        if (!PayRun.STATE_POSTED.equals(payRun.getState())) {
            throw new HrDomainException("Only posted pay runs can be paid");
        }
        UUID companyId = payRun.getCompanyId().getId();
        UUID salariesPayableAccount = accountingReferenceLookupPort.resolveAccountIdByCode(companyId, SALARIES_PAYABLE);
        UUID bankAccount = accountingReferenceLookupPort.resolveLiquidityAccountIdForJournal(companyId, cmd.bankJournalId());

        BigDecimal totalNet = payRun.getPayslips().stream()
                .map(Payslip::getNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String currencyCode = payRun.getPayslips().isEmpty()
                ? companyCurrencyPort.defaultCurrencyCode(payRun.getCompanyId())
                : payRun.getPayslips().get(0).getCurrencyCode();
        LocalDate paymentDate = cmd.paymentDate() != null ? cmd.paymentDate() : payRun.getPeriodEnd();

        List<JournalItemCommand> items = new ArrayList<>();
        items.add(new JournalItemCommand(
                salariesPayableAccount, "Salaries payable", totalNet, BigDecimal.ZERO,
                currencyCode, totalNet, null));
        items.add(new JournalItemCommand(
                bankAccount, "Payroll payment", BigDecimal.ZERO, totalNet,
                currencyCode, totalNet.negate(), null));

        CreateJournalEntryCommand jcmd = new CreateJournalEntryCommand(
                companyId,
                cmd.bankJournalId(),
                cmd.reference() != null ? cmd.reference() : "",
                paymentDate,
                currencyCode,
                null,
                items);
        CreateJournalEntryResponse payEntry = journalEntryApplicationService.createJournalEntry(jcmd);
        journalEntryApplicationService.postJournalEntry(payEntry.getJournalEntryId());

        PayRun paid = payRun.markPaid(payEntry.getJournalEntryId());
        return toResponse(payRunRepository.save(paid));
    }

    private Payslip computePayslipForContract(PayRun payRun, ContractRow contract) {
        StructureRow structure = payrollPersistence.findStructure(contract.structureId())
                .orElseThrow(() -> new HrDomainException("Structure not found: " + contract.structureId()));
        BigDecimal expectedDays = payrollPersistence.countExpectedWorkDays(
                contract.workingScheduleId(), payRun.getPeriodStart(), payRun.getPeriodEnd());
        BigDecimal workedDays = payrollPersistence.countWorkedDays(
                contract.employeeId(), payRun.getPeriodStart(), payRun.getPeriodEnd());
        BigDecimal absenceDays = expectedDays.subtract(workedDays).max(BigDecimal.ZERO);

        List<SalaryRuleInput> rules = structure.rules().stream()
                .map(r -> new SalaryRuleInput(
                        r.id(), r.code(), r.name(), r.category(), r.amountType(),
                        r.amount(), r.active(), r.accountId()))
                .toList();

        PayslipComputationInput input = new PayslipComputationInput(
                payRun.getId(),
                contract.employeeId(),
                contract.id(),
                contract.currencyCode(),
                contract.wage(),
                contract.attendanceBased(),
                expectedDays,
                workedDays,
                absenceDays,
                rules);

        return payrollDomainService.computePayslip(input);
    }

    private PayRun loadRun(UUID id) {
        return payRunRepository.findById(id)
                .orElseThrow(() -> new HrDomainException("Pay run not found: " + id));
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

    private PayRunResponse toResponse(PayRun run) {
        List<PayslipResponse> payslips = run.getPayslips().stream().map(this::toPayslipResponse).toList();
        return new PayRunResponse(
                run.getId(),
                run.getCompanyId().getId(),
                run.getName(),
                run.getPeriodStart(),
                run.getPeriodEnd(),
                run.getState(),
                run.getJournalEntryId(),
                run.getPaymentJournalEntryId(),
                payslips);
    }

    private PayslipResponse toPayslipResponse(Payslip payslip) {
        String employeeName = payrollPersistence.findEmployeeName(payslip.getEmployeeId().getId()).orElse(null);
        List<PayslipLineDetailResponse> lines = payslip.getLines().stream()
                .map(l -> new PayslipLineDetailResponse(
                        l.getId(), l.getCode(), l.getName(), l.getCategory(), l.getAmount(), l.getAccountId()))
                .toList();
        return new PayslipResponse(
                payslip.getId(),
                payslip.getPayRunId(),
                payslip.getEmployeeId().getId(),
                employeeName,
                payslip.getContractId(),
                payslip.getCurrencyCode(),
                payslip.getState(),
                payslip.getBasic(),
                payslip.getAllowances(),
                payslip.getDeductions(),
                payslip.getNet(),
                payslip.getWorkedDays(),
                payslip.getAbsenceDays(),
                lines);
    }
}
