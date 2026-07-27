package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateAttendanceCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.AttendanceApplicationService;
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

import java.time.*;
import java.util.List;
import java.util.UUID;

/**
 * Demo attendance records for gantt/list views. Runs after {@link DemoHrDataSeeder}.
 */
@Component
@Order(32)
@ConditionalOnProperty(name = "hr.seed.demo-data", havingValue = "true", matchIfMissing = false)
public class DemoAttendanceDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAttendanceDataSeeder.class);
    private static final UUID COMPANY_ID = PlatformRbacSeeder.DEFAULT_COMPANY_ID;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final String MARKER_EMPLOYEE = "Demo HR Manager (Seed)";

    private final AttendanceApplicationService attendanceService;
    private final EmployeeApplicationService employeeService;

    public DemoAttendanceDataSeeder(AttendanceApplicationService attendanceService,
                                    EmployeeApplicationService employeeService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var employees = employeeService.search(
                new CompanyId(COMPANY_ID), null, null, true, PageRequest.of(0, 500));
        if (employees.isEmpty()) {
            log.info("No employees found; skipping attendance demo seed");
            return;
        }

        var markerEmp = employees.getContent().stream()
                .filter(e -> MARKER_EMPLOYEE.equals(e.displayName()))
                .findFirst()
                .orElse(employees.getContent().get(0));

        var markerDay = LocalDate.of(2026, 5, 10).atTime(11, 0).atZone(ZONE).toInstant();
        var existing = attendanceService.search(
                new CompanyId(COMPANY_ID),
                markerEmp.id(),
                markerDay,
                markerDay.plus(Duration.ofDays(1)),
                PageRequest.of(0, 1));
        if (!existing.isEmpty()) {
            log.info("Demo attendance data already seeded; skipping");
            return;
        }

        log.info("Seeding demo attendance records for company {}", COMPANY_ID);

        UUID managerId = markerEmp.id();
        seedMayManagerRecords(managerId);

        var allIds = employees.getContent().stream().map(e -> e.id()).toList();
        seedJuneWeekRecords(allIds);

        log.info("Demo attendance seed complete");
    }

    private void seedMayManagerRecords(UUID employeeId) {
        int[][] schedule = {
                {10, 11, 0, 20, 0},
                {9, 9, 0, 17, 30},
                {8, 8, 30, 17, 0},
                {7, 9, 15, 18, 45},
                {6, 10, 0, 19, 0},
                {5, 8, 45, 16, 15},
                {4, 9, 30, 17, 30},
                {3, 10, 15, 18, 0},
                {2, 8, 0, 16, 30},
                {1, 11, 0, 20, 0},
        };
        for (int[] row : schedule) {
            int day = row[0];
            int inH = row[1], inM = row[2], outH = row[3], outM = row[4];
            LocalDate date = LocalDate.of(2026, 5, day);
            create(employeeId,
                    date.atTime(inH, inM).atZone(ZONE).toInstant(),
                    date.atTime(outH, outM).atZone(ZONE).toInstant());
        }
    }

    private void seedJuneWeekRecords(List<UUID> employeeIds) {
        LocalDate weekStart = LocalDate.of(2026, 6, 14);
        for (int i = 0; i < employeeIds.size(); i++) {
            UUID empId = employeeIds.get(i);
            for (int d = 0; d < 5; d++) {
                LocalDate day = weekStart.plusDays(d);
                int startHour = 8 + (i % 3);
                int endHour = 16 + (i % 4);
                create(empId,
                        day.atTime(startHour, 11).atZone(ZONE).toInstant(),
                        day.atTime(endHour, 11).atZone(ZONE).toInstant());
            }
            if (i % 4 == 1) {
                LocalDate sat = LocalDate.of(2026, 6, 20);
                create(empId,
                        sat.atTime(4, 11).atZone(ZONE).toInstant(),
                        sat.atTime(16, 11).atZone(ZONE).toInstant());
            }
        }
    }

    private void create(UUID employeeId, Instant checkIn, Instant checkOut) {
        CreateAttendanceCommand cmd = new CreateAttendanceCommand();
        cmd.setCompanyId(COMPANY_ID);
        cmd.setEmployeeId(employeeId);
        cmd.setCheckIn(checkIn);
        cmd.setCheckOut(checkOut);
        cmd.setCheckInMode("manual");
        cmd.setCheckOutMode("manual");
        attendanceService.create(cmd);
    }
}
