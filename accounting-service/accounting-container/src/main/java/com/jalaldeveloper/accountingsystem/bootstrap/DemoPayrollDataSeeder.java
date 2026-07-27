package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.payroll.PayrollApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.EmployeeApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.PayrollApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.accounting.CompanyCurrencyPort;
import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Demo payroll configuration (40h/week schedule, basic salary structure, sample contract).
 */
@Component
@Order(33)
@ConditionalOnProperty(name = "hr.seed.demo-data", havingValue = "true", matchIfMissing = false)
public class DemoPayrollDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPayrollDataSeeder.class);
    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final String MARKER_SCHEDULE = "40 hours/week";

    private final PayrollApplicationService payrollService;
    private final EmployeeApplicationService employeeService;
    private final CompanyCurrencyPort companyCurrencyPort;

    public DemoPayrollDataSeeder(PayrollApplicationService payrollService,
                                 EmployeeApplicationService employeeService,
                                 CompanyCurrencyPort companyCurrencyPort) {
        this.payrollService = payrollService;
        this.employeeService = employeeService;
        this.companyCurrencyPort = companyCurrencyPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        var existing = payrollService.listWorkingSchedules(new CompanyId(COMPANY_ID));
        if (existing.stream().anyMatch(s -> MARKER_SCHEDULE.equals(s.name()))) {
            log.info("Demo payroll data already seeded; skipping");
            return;
        }

        CompanyId companyId = new CompanyId(COMPANY_ID);
        String currencyCode = companyCurrencyPort.defaultCurrencyCode(companyId);
        log.info("Seeding demo payroll data for company {} (currency {})", COMPANY_ID, currencyCode);

        WorkingScheduleResponse schedule = payrollService.createWorkingSchedule(new SaveWorkingScheduleCommand(
                COMPANY_ID,
                MARKER_SCHEDULE,
                false,
                0,
                List.of(
                        line(7, "8"),
                        line(1, "8"),
                        line(2, "8"),
                        line(3, "8"),
                        line(4, "8")
                )));

        EmployeeTypeResponse employeeType = payrollService.createEmployeeType(new SaveEmployeeTypeCommand(
                COMPANY_ID, "Employee", null, 0));

        payrollService.createEmployeeType(new SaveEmployeeTypeCommand(
                COMPANY_ID, "Worker", null, 10));

        StructureTypeResponse structureType = payrollService.createStructureType(new SaveStructureTypeCommand(
                COMPANY_ID,
                "Standard Employee",
                "month",
                "fixed",
                schedule.id(),
                null,
                null,
                0));

        StructureResponse structure = payrollService.createStructure(new SaveStructureCommand(
                COMPANY_ID,
                "Regular Pay",
                structureType.id(),
                "month",
                true,
                null,
                0,
                List.of(
                        rule("Basic Salary", "BASIC", "basic", "fixed", "0", 10),
                        rule("Transport Allowance", "TRANSPORT", "allowance", "fixed", demoTransport(currencyCode), 20),
                        rule("Housing Allowance", "HOUSING", "allowance", "percent", "25", 30),
                        rule("Social Security", "SOCIAL", "deduction", "percent", "12", 40),
                        rule("Net Salary", "NET", "net", "fixed", "0", 100)
                )));

        payrollService.updateStructureType(structureType.id(), new SaveStructureTypeCommand(
                COMPANY_ID,
                "Standard Employee",
                "month",
                "fixed",
                schedule.id(),
                null,
                structure.id(),
                0));

        var employees = employeeService.search(companyId, null, null, true, PageRequest.of(0, 20));
        if (!employees.isEmpty()) {
            var emp = employees.getContent().get(0);
            payrollService.createContract(new SaveContractCommand(
                    COMPANY_ID,
                    emp.id(),
                    "Contract " + emp.displayName(),
                    employeeType.id(),
                    structure.id(),
                    schedule.id(),
                    demoMonthlyWage(currencyCode),
                    "fixed",
                    currencyCode,
                    LocalDate.of(2026, 1, 1),
                    null,
                    "running",
                    true));
        }

        log.info("Demo payroll seed complete");
    }

    private static BigDecimal demoMonthlyWage(String currencyCode) {
        return "IQD".equalsIgnoreCase(currencyCode)
                ? new BigDecimal("2500000")
                : new BigDecimal("5000");
    }

    private static String demoTransport(String currencyCode) {
        return "IQD".equalsIgnoreCase(currencyCode) ? "150000" : "200";
    }

    private static WorkingScheduleLineCommand line(int dayOfWeek, String hours) {
        return new WorkingScheduleLineCommand(dayOfWeek, new BigDecimal(hours), null);
    }

    private static SalaryRuleCommand rule(String name, String code, String category,
                                          String amountType, String amount, int sequence) {
        return new SalaryRuleCommand(null, name, code, category, amountType, new BigDecimal(amount), sequence, true, null);
    }
}
