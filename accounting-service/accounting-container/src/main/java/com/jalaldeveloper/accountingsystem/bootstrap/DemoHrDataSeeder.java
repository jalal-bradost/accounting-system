package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateDepartmentCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateEmployeeCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.DepartmentApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.EmployeeApplicationService;
import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Optional demo HR dataset: departments and employees. Idempotent via marker employee name.
 */
@Component
@Order(31)
@ConditionalOnProperty(name = "hr.seed.demo-data", havingValue = "true", matchIfMissing = false)
public class DemoHrDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoHrDataSeeder.class);

    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final String MARKER_EMPLOYEE = "Demo HR Manager (Seed)";

    private final DepartmentApplicationService departmentService;
    private final EmployeeApplicationService employeeService;

    public DemoHrDataSeeder(DepartmentApplicationService departmentService,
                            EmployeeApplicationService employeeService) {
        this.departmentService = departmentService;
        this.employeeService = employeeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var existing = employeeService.search(
                new com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId(COMPANY_ID),
                MARKER_EMPLOYEE, null, true, PageRequest.of(0, 1));
        if (!existing.isEmpty()) {
            log.info("Demo HR data already seeded (marker employee found); skipping");
            return;
        }

        log.info("Seeding demo HR departments and employees for company {}", COMPANY_ID);

        var management = departmentService.create(deptCmd("Management", 0));
        var sales = departmentService.create(deptCmd("Sales", 1));
        var admin = departmentService.create(deptCmd("Administration", 2));

        var ceo = employeeService.create(employeeCmd(
                MARKER_EMPLOYEE,
                "ceo@demo.local",
                "Chief Executive Officer",
                management.id(),
                null,
                LocalDate.of(2020, 1, 15)));

        var salesLead = employeeService.create(employeeCmd(
                "Demo Sales Lead (Seed)",
                "sales.lead@demo.local",
                "Sales Manager",
                sales.id(),
                ceo.id(),
                LocalDate.of(2021, 3, 1)));

        employeeService.create(employeeCmd(
                "Demo Sales Rep (Seed)",
                "sales.rep@demo.local",
                "Sales Representative",
                sales.id(),
                salesLead.id(),
                LocalDate.of(2022, 6, 10)));

        employeeService.create(employeeCmd(
                "Demo Admin Assistant (Seed)",
                "admin.assistant@demo.local",
                "Administrative Assistant",
                admin.id(),
                ceo.id(),
                LocalDate.of(2023, 2, 20)));

        employeeService.create(employeeCmd(
                "Demo HR Coordinator (Seed)",
                "hr@demo.local",
                "HR Coordinator",
                admin.id(),
                ceo.id(),
                LocalDate.of(2023, 9, 5)));

        departmentService.update(management.id(), updateDeptManager(ceo.id()));
        departmentService.update(sales.id(), updateDeptManagerAndParent(salesLead.id(), management.id()));
        departmentService.update(admin.id(), updateDeptManagerAndParent(ceo.id(), management.id()));

        log.info("Demo HR seed complete");
    }

    private static CreateDepartmentCommand deptCmd(String name, int colorIndex) {
        CreateDepartmentCommand cmd = new CreateDepartmentCommand();
        cmd.setCompanyId(COMPANY_ID);
        cmd.setName(name);
        cmd.setColorIndex(colorIndex);
        return cmd;
    }

    private static com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateDepartmentCommand updateDeptManager(UUID managerId) {
        var cmd = new com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateDepartmentCommand();
        cmd.setManagerId(managerId);
        return cmd;
    }

    private static com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateDepartmentCommand updateDeptManagerAndParent(
            UUID managerId, UUID parentId) {
        var cmd = new com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateDepartmentCommand();
        cmd.setManagerId(managerId);
        cmd.setParentId(parentId);
        return cmd;
    }

    private static CreateEmployeeCommand employeeCmd(String displayName,
                                                     String email,
                                                     String jobTitle,
                                                     UUID departmentId,
                                                     UUID managerId,
                                                     LocalDate hireDate) {
        CreateEmployeeCommand cmd = new CreateEmployeeCommand();
        cmd.setCompanyId(COMPANY_ID);
        cmd.setDisplayName(displayName);
        cmd.setWorkEmail(email);
        cmd.setJobTitle(jobTitle);
        cmd.setDepartmentId(departmentId);
        cmd.setManagerId(managerId);
        cmd.setHireDate(hireDate);
        cmd.setWorkLocation("HQ");
        return cmd;
    }
}
