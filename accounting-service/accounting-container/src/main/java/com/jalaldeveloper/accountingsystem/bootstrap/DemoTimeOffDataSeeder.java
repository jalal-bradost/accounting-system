package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.MandatoryDayEntity;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PublicHolidayEntity;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.MandatoryDayJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.PublicHolidayJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.timeoff.TimeOffApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.EmployeeApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.TimeOffApplicationService;
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
import java.util.UUID;

@Component
@Order(34)
@ConditionalOnProperty(name = "hr.seed.demo-data", havingValue = "true", matchIfMissing = false)
public class DemoTimeOffDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoTimeOffDataSeeder.class);
    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final String MARKER_TYPE = "Paid Time Off";

    private final TimeOffApplicationService timeOffService;
    private final EmployeeApplicationService employeeService;
    private final PublicHolidayJpaRepository publicHolidayRepository;
    private final MandatoryDayJpaRepository mandatoryDayRepository;

    public DemoTimeOffDataSeeder(TimeOffApplicationService timeOffService,
                                 EmployeeApplicationService employeeService,
                                 PublicHolidayJpaRepository publicHolidayRepository,
                                 MandatoryDayJpaRepository mandatoryDayRepository) {
        this.timeOffService = timeOffService;
        this.employeeService = employeeService;
        this.publicHolidayRepository = publicHolidayRepository;
        this.mandatoryDayRepository = mandatoryDayRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (timeOffService.listTypes(new CompanyId(COMPANY_ID)).stream()
                .anyMatch(t -> MARKER_TYPE.equals(t.name()))) {
            log.info("Demo time off data already seeded; skipping");
            return;
        }

        log.info("Seeding demo time off data");

        var pto = timeOffService.createType(new SaveTimeOffTypeCommand(
                COMPANY_ID, "Paid Time Off", "LEAVE120", "PTO", "IQ", "#E8A598", 0, true));
        var sick = timeOffService.createType(new SaveTimeOffTypeCommand(
                COMPANY_ID, "Sick Time Off", "LEAVE110", "STO", "IQ", "#F7CD72", 10, true));
        timeOffService.createType(new SaveTimeOffTypeCommand(
                COMPANY_ID, "Unpaid", "LEAVE90", "UN", "IQ", "#F06292", 20, true));
        timeOffService.createType(new SaveTimeOffTypeCommand(
                COMPANY_ID, "Training Time Off", "LEAVE100", "TR", null, "#FFB74D", 30, true));

        var employees = employeeService.search(
                new CompanyId(COMPANY_ID), null, null, true, PageRequest.of(0, 10));
        if (employees.isEmpty()) {
            log.warn("No employees for time off demo seed");
            return;
        }

        UUID employeeId = employees.getContent().get(0).id();
        LocalDate yearStart = LocalDate.of(2026, 1, 1);
        LocalDate yearEnd = LocalDate.of(2026, 12, 31);

        var allEmployees = employees.getContent();
        for (int i = 0; i < Math.min(allEmployees.size(), 3); i++) {
            UUID empId = allEmployees.get(i).id();
            var ptoAlloc = timeOffService.createAllocation(new SaveAllocationCommand(
                    COMPANY_ID, empId, pto.id(),
                    "Paid Time Off" + (i > 0 ? " (" + allEmployees.get(i).displayName() + ")" : " (Iraq)"),
                    new BigDecimal(i == 0 ? "20" : "15"),
                    "regular", yearStart, yearEnd));
            if (i < 2) {
                timeOffService.approveAllocation(ptoAlloc.id());
            }

            if (i == 0) {
                var sickAlloc = timeOffService.createAllocation(new SaveAllocationCommand(
                        COMPANY_ID, empId, sick.id(),
                        "Sick Time Off", new BigDecimal("10"),
                        "regular", yearStart, yearEnd));
                timeOffService.approveAllocation(sickAlloc.id());

                timeOffService.createAllocation(new SaveAllocationCommand(
                        COMPANY_ID, empId,
                        timeOffService.listTypes(new CompanyId(COMPANY_ID)).stream()
                                .filter(t -> "LEAVE100".equals(t.code())).findFirst().orElseThrow().id(),
                        "Training Time Off", new BigDecimal("7"),
                        "regular", yearStart, yearEnd));

                timeOffService.createRequest(new SaveLeaveRequestCommand(
                        COMPANY_ID, empId, pto.id(),
                        LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 24), null, "Summer break"));
            }
        }

        seedHoliday("Public Time Off", LocalDate.of(2026, 2, 13), "IQ");
        seedMandatory("Company Celebration", LocalDate.of(2026, 6, 27));

        log.info("Demo time off seed complete");
    }

    private void seedHoliday(String name, LocalDate date, String country) {
        if (publicHolidayRepository.findByCompanyIdAndHolidayDateBetweenOrderByHolidayDateAsc(
                COMPANY_ID, date, date).isEmpty()) {
            PublicHolidayEntity h = new PublicHolidayEntity();
            h.setId(UUID.randomUUID());
            h.setCompanyId(COMPANY_ID);
            h.setName(name);
            h.setHolidayDate(date);
            h.setCountryCode(country);
            publicHolidayRepository.save(h);
        }
    }

    private void seedMandatory(String name, LocalDate date) {
        if (mandatoryDayRepository.findByCompanyIdAndMandatoryDateBetweenOrderByMandatoryDateAsc(
                COMPANY_ID, date, date).isEmpty()) {
            MandatoryDayEntity m = new MandatoryDayEntity();
            m.setId(UUID.randomUUID());
            m.setCompanyId(COMPANY_ID);
            m.setName(name);
            m.setMandatoryDate(date);
            mandatoryDayRepository.save(m);
        }
    }
}
