package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateEmployeeCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateEmployeeCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.EmployeeApplicationService;
import com.jalaldeveloper.accountingsystem.platform.bootstrap.PlatformRbacSeeder;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AppUserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Links demo employees to platform users by matching work email when both demo HR and user seeders ran.
 */
@Component
@Order(32)
@ConditionalOnProperty(name = "hr.seed.demo-data", havingValue = "true", matchIfMissing = false)
public class DemoEmployeeUserLinkSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoEmployeeUserLinkSeeder.class);
    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;

    private static final List<String> DEMO_EMAILS = List.of(
            "ceo@demo.local",
            "sales.lead@demo.local",
            "sales.rep@demo.local",
            "admin.assistant@demo.local",
            "hr@demo.local");

    private final EmployeeApplicationService employeeService;
    private final AppUserJpaRepository appUserRepository;

    public DemoEmployeeUserLinkSeeder(EmployeeApplicationService employeeService,
                                      AppUserJpaRepository appUserRepository) {
        this.employeeService = employeeService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        CompanyId companyId = new CompanyId(COMPANY_ID);
        for (String email : DEMO_EMAILS) {
            var userOpt = appUserRepository.findByCompanyIdAndEmail(COMPANY_ID, email);
            if (userOpt.isEmpty()) {
                continue;
            }
            AppUserEntity user = userOpt.get();
            var employees = employeeService.search(companyId, email, null, true, PageRequest.of(0, 5));
            employees.forEach(emp -> {
                if (emp.workEmail() != null && emp.workEmail().equalsIgnoreCase(email)) {
                    UpdateEmployeeCommand cmd = new UpdateEmployeeCommand();
                    cmd.setUserId(user.getId());
                    employeeService.update(emp.id(), cmd);
                    log.info("Linked employee {} to user {}", emp.displayName(), user.getUsername());
                }
            });
        }
    }
}
