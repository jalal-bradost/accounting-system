package com.jalaldeveloper.accountingsystem.hr.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Attendance;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.AttendanceId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.AttendanceApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.AttendanceRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.EmployeeRepository;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence.ScheduleLineRow;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence.ScheduleRow;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Validated
class AttendanceApplicationServiceImpl implements AttendanceApplicationService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm:ss a");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollPersistence payrollPersistence;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    AttendanceApplicationServiceImpl(AttendanceRepository attendanceRepository,
                                     EmployeeRepository employeeRepository,
                                     PayrollPersistence payrollPersistence,
                                     ObjectProvider<CompanyContext> companyContextProvider) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.payrollPersistence = payrollPersistence;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public AttendanceResponse create(CreateAttendanceCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.getCompanyId());
        ensureEmployeeExists(cmd.getEmployeeId());
        UUID id = UUID.randomUUID();
        Attendance attendance = Attendance.builder()
                .id(new AttendanceId(id))
                .companyId(companyId)
                .employeeId(new EmployeeId(cmd.getEmployeeId()))
                .checkIn(cmd.getCheckIn())
                .checkOut(cmd.getCheckOut())
                .checkInMode(normalizeMode(cmd.getCheckInMode()))
                .checkOutMode(cmd.getCheckOut() != null ? normalizeMode(cmd.getCheckOutMode()) : null)
                .extraHoursMinutes(cmd.getExtraHoursMinutes() != null ? cmd.getExtraHoursMinutes() : 0)
                .build();
        attendance.validate();
        return toResponse(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponse update(UUID id, UpdateAttendanceCommand cmd) {
        Attendance existing = loadOrThrow(id);
        UUID employeeId = cmd.getEmployeeId() != null ? cmd.getEmployeeId() : existing.getEmployeeId().getId();
        if (cmd.getEmployeeId() != null) ensureEmployeeExists(employeeId);

        Instant checkOut = existing.getCheckOut();
        if (Boolean.TRUE.equals(cmd.getCheckOutReset())) {
            checkOut = null;
        } else if (cmd.getCheckOut() != null) {
            checkOut = cmd.getCheckOut();
        }

        Attendance updated = Attendance.builder()
                .id(existing.getId())
                .companyId(existing.getCompanyId())
                .employeeId(new EmployeeId(employeeId))
                .checkIn(cmd.getCheckIn() != null ? cmd.getCheckIn() : existing.getCheckIn())
                .checkOut(checkOut)
                .checkInMode(cmd.getCheckInMode() != null ? normalizeMode(cmd.getCheckInMode()) : existing.getCheckInMode())
                .checkOutMode(checkOut != null
                        ? normalizeMode(cmd.getCheckOutMode() != null ? cmd.getCheckOutMode() : existing.getCheckOutMode())
                        : null)
                .extraHoursMinutes(cmd.getExtraHoursMinutes() != null ? cmd.getExtraHoursMinutes() : existing.getExtraHoursMinutes())
                .build();
        updated.validate();
        return toResponse(attendanceRepository.save(updated));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse get(UUID id) {
        return toResponse(loadOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceSummaryResponse> search(CompanyId companyId,
                                                  UUID employeeId,
                                                  Instant from,
                                                  Instant to,
                                                  Pageable pageable) {
        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now().plus(Duration.ofDays(365));
        Page<Attendance> page = attendanceRepository.search(companyId, employeeId, rangeFrom, rangeTo, pageable);
        return page.map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> gantt(CompanyId companyId,
                                                 UUID employeeId,
                                                 Instant from,
                                                 Instant to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        return attendanceRepository.findForGantt(companyId, employeeId, from, to).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public List<AttendanceResponse> bulkCreate(BulkCreateAttendanceCommand cmd) {
        if (cmd.getItems() == null || cmd.getItems().isEmpty()) {
            throw new HrDomainException("At least one attendance item is required");
        }
        List<AttendanceResponse> created = new ArrayList<>();
        for (CreateAttendanceCommand item : cmd.getItems()) {
            if (item.getCompanyId() == null && cmd.getCompanyId() != null) {
                item.setCompanyId(cmd.getCompanyId());
            }
            created.add(create(item));
        }
        return created;
    }

    @Override
    @Transactional
    public List<AttendanceResponse> generateFromSchedule(GenerateAttendanceFromScheduleCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.getCompanyId());
        if (cmd.getEmployeeId() == null) {
            throw new HrDomainException("employeeId required");
        }
        ensureEmployeeExists(cmd.getEmployeeId());
        ScheduleRow schedule = payrollPersistence.findSchedule(cmd.getWorkingScheduleId())
                .orElseThrow(() -> new HrDomainException("Working schedule not found: " + cmd.getWorkingScheduleId()));
        if (!schedule.companyId().equals(companyId.getId())) {
            throw new HrDomainException("Working schedule company mismatch");
        }

        Set<Short> workDays = new HashSet<>();
        Map<Short, BigDecimal> hoursByDay = new HashMap<>();
        for (ScheduleLineRow line : schedule.lines()) {
            if (line.hours().signum() > 0) {
                workDays.add(line.dayOfWeek());
                hoursByDay.put(line.dayOfWeek(), line.hours());
            }
        }

        List<AttendanceResponse> created = new ArrayList<>();
        ZoneId zone = DEFAULT_ZONE;
        for (LocalDate d = cmd.getFromDate(); !d.isAfter(cmd.getToDate()); d = d.plusDays(1)) {
            short dow = (short) d.getDayOfWeek().getValue();
            if (!workDays.contains(dow)) {
                continue;
            }
            BigDecimal hours = hoursByDay.getOrDefault(dow, BigDecimal.valueOf(8));
            int minutes = hours.multiply(BigDecimal.valueOf(60)).intValue();
            Instant checkIn = d.atTime(cmd.getDefaultStartHour(), cmd.getDefaultStartMinute()).atZone(zone).toInstant();
            Instant checkOut = checkIn.plus(Duration.ofMinutes(minutes));
            CreateAttendanceCommand item = new CreateAttendanceCommand();
            item.setCompanyId(companyId.getId());
            item.setEmployeeId(cmd.getEmployeeId());
            item.setCheckIn(checkIn);
            item.setCheckOut(checkOut);
            item.setCheckInMode("schedule");
            item.setCheckOutMode("schedule");
            created.add(create(item));
        }
        return created;
    }

    private Attendance loadOrThrow(UUID id) {
        return attendanceRepository.findById(new AttendanceId(id))
                .orElseThrow(() -> new HrDomainException("Attendance not found: " + id));
    }

    private void ensureEmployeeExists(UUID employeeId) {
        employeeRepository.findById(new EmployeeId(employeeId))
                .orElseThrow(() -> new HrDomainException("Employee not found: " + employeeId));
    }

    private CompanyId resolveCompany(UUID companyId) {
        if (companyId != null) return new CompanyId(companyId);
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.requireCompany();
        }
        throw new HrDomainException("companyId required");
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return "manual";
        return mode.toLowerCase(Locale.ROOT);
    }

    private AttendanceResponse toResponse(Attendance a) {
        EmployeeDisplayMeta meta = employeeMeta(a.getEmployeeId().getId());
        int worked = workedMinutes(a.getCheckIn(), a.getCheckOut());
        return new AttendanceResponse(
                a.getId().getId(),
                a.getCompanyId().getId(),
                a.getEmployeeId().getId(),
                meta.displayName(),
                meta.imageUrl(),
                a.getCheckIn(),
                a.getCheckOut(),
                capitalizeMode(a.getCheckInMode()),
                a.getCheckOutMode() != null ? capitalizeMode(a.getCheckOutMode()) : null,
                worked,
                a.getExtraHoursMinutes(),
                formatDuration(worked),
                buildDisplayTitle(worked, a.getCheckIn(), a.getCheckOut()));
    }

    private AttendanceSummaryResponse toSummary(Attendance a) {
        EmployeeDisplayMeta meta = employeeMeta(a.getEmployeeId().getId());
        int worked = workedMinutes(a.getCheckIn(), a.getCheckOut());
        return new AttendanceSummaryResponse(
                a.getId().getId(),
                a.getEmployeeId().getId(),
                meta.displayName(),
                meta.imageUrl(),
                a.getCheckIn(),
                a.getCheckOut(),
                worked,
                a.getExtraHoursMinutes(),
                formatDuration(worked),
                buildDisplayTitle(worked, a.getCheckIn(), a.getCheckOut()));
    }

    private EmployeeDisplayMeta employeeMeta(UUID employeeId) {
        Map<UUID, EmployeeDisplayMeta> meta = employeeRepository.findDisplayMetaByEmployeeIds(Set.of(employeeId));
        EmployeeDisplayMeta m = meta.get(employeeId);
        if (m == null) return new EmployeeDisplayMeta("Unknown", null);
        return m;
    }

    static int workedMinutes(Instant checkIn, Instant checkOut) {
        if (checkOut == null) return 0;
        return (int) Duration.between(checkIn, checkOut).toMinutes();
    }

    static String formatDuration(int minutes) {
        if (minutes <= 0) return "0h";
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (mins == 0) return hours + "h";
        if (hours == 0) return mins + "m";
        return hours + "h " + mins + "m";
    }

    static String formatDurationClock(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    static String buildDisplayTitle(int workedMinutes, Instant checkIn, Instant checkOut) {
        String duration = formatDurationClock(workedMinutes);
        if (checkOut == null) {
            return duration + " (Open)";
        }
        LocalDateTime in = LocalDateTime.ofInstant(checkIn, DEFAULT_ZONE);
        LocalDateTime out = LocalDateTime.ofInstant(checkOut, DEFAULT_ZONE);
        return duration + " (" + TIME_FMT.format(in) + "-" + TIME_FMT.format(out) + ")";
    }

    private static String capitalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return "Manual";
        return mode.substring(0, 1).toUpperCase(Locale.ROOT) + mode.substring(1).toLowerCase(Locale.ROOT);
    }
}
