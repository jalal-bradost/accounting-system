package com.jalaldeveloper.accountingsystem.hr.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.LeaveAllocation;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.LeaveRequest;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.TimeOffType;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeDisplayMeta;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.timeoff.TimeOffApi.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input.TimeOffApplicationService;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.*;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Validated
class TimeOffApplicationServiceImpl implements TimeOffApplicationService {

    private final TimeOffTypeRepository typeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveRequestRepository requestRepository;
    private final TimeOffHolidayRepository holidayRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    TimeOffApplicationServiceImpl(TimeOffTypeRepository typeRepository,
                                  LeaveAllocationRepository allocationRepository,
                                  LeaveRequestRepository requestRepository,
                                  TimeOffHolidayRepository holidayRepository,
                                  EmployeeRepository employeeRepository,
                                  ObjectProvider<CompanyContext> companyContextProvider) {
        this.typeRepository = typeRepository;
        this.allocationRepository = allocationRepository;
        this.requestRepository = requestRepository;
        this.holidayRepository = holidayRepository;
        this.employeeRepository = employeeRepository;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeOffTypeResponse> listTypes(CompanyId companyId) {
        return typeRepository.findByCompany(companyId).stream()
                .filter(t -> !t.isCompensatory())
                .map(this::toTypeResponse)
                .toList();
    }

    @Override
    @Transactional
    public TimeOffTypeResponse createType(SaveTimeOffTypeCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        if (typeRepository.existsByCompanyAndCode(companyId, cmd.code())) {
            throw new HrDomainException("Time off type code already exists: " + cmd.code());
        }
        TimeOffType type = TimeOffType.builder()
                .id(new TimeOffTypeId(UUID.randomUUID()))
                .companyId(companyId)
                .name(cmd.name())
                .code(cmd.code())
                .displayCode(cmd.displayCode())
                .countryCode(cmd.countryCode())
                .colorHex(cmd.colorHex() != null ? cmd.colorHex() : "#714B67")
                .sortOrder(cmd.sortOrder() != null ? cmd.sortOrder() : 0)
                .active(cmd.active() == null || cmd.active())
                .build();
        type.validate();
        return toTypeResponse(typeRepository.save(type));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationResponse> listAllocations(CompanyId companyId, UUID employeeId) {
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(allocationRepository.search(companyId, employeeId).stream()
                .map(a -> a.getEmployeeId().getId()).collect(Collectors.toSet()));
        Map<UUID, TimeOffType> types = loadTypes(companyId);
        return allocationRepository.search(companyId, employeeId).stream()
                .filter(a -> !isCompensatoryType(types.get(a.getTimeOffTypeId().getId())))
                .map(a -> toAllocationResponse(a, meta, types, companyId))
                .toList();
    }

    @Override
    @Transactional
    public AllocationResponse createAllocation(SaveAllocationCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        ensureEmployee(cmd.employeeId());
        TimeOffType type = loadType(cmd.timeOffTypeId());
        if (type.isCompensatory()) throw new HrDomainException("Compensatory time off is not supported");
        LeaveAllocation allocation = LeaveAllocation.builder()
                .id(new LeaveAllocationId(UUID.randomUUID()))
                .companyId(companyId)
                .employeeId(new EmployeeId(cmd.employeeId()))
                .timeOffTypeId(new TimeOffTypeId(cmd.timeOffTypeId()))
                .name(cmd.name())
                .numberOfDays(cmd.numberOfDays())
                .allocationType(cmd.allocationType() != null ? cmd.allocationType() : "regular")
                .state(LeaveAllocation.STATE_CONFIRM)
                .dateFrom(cmd.dateFrom())
                .dateTo(cmd.dateTo())
                .build();
        allocation.validate();
        LeaveAllocation saved = allocationRepository.save(allocation);
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(Set.of(saved.getEmployeeId().getId()));
        Map<UUID, TimeOffType> types = loadTypes(companyId);
        return toAllocationResponse(saved, meta, types, companyId);
    }

    @Override
    @Transactional
    public AllocationResponse approveAllocation(UUID id) {
        LeaveAllocation allocation = loadAllocation(id);
        LeaveAllocation saved = allocationRepository.save(allocation.approve());
        CompanyId companyId = allocation.getCompanyId();
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(Set.of(saved.getEmployeeId().getId()));
        Map<UUID, TimeOffType> types = loadTypes(companyId);
        return toAllocationResponse(saved, meta, types, companyId);
    }

    @Override
    @Transactional
    public AllocationResponse refuseAllocation(UUID id) {
        LeaveAllocation allocation = loadAllocation(id);
        LeaveAllocation saved = allocationRepository.save(allocation.refuse());
        CompanyId companyId = allocation.getCompanyId();
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(Set.of(saved.getEmployeeId().getId()));
        Map<UUID, TimeOffType> types = loadTypes(companyId);
        return toAllocationResponse(saved, meta, types, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> listRequests(CompanyId companyId, UUID employeeId, String state, int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        Map<UUID, TimeOffType> types = loadTypes(companyId);
        List<LeaveRequest> requests = requestRepository.search(companyId, employeeId, state, from, to);
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(requests.stream()
                .map(r -> r.getEmployeeId().getId()).collect(Collectors.toSet()));
        return requests.stream()
                .filter(r -> !isCompensatoryType(types.get(r.getTimeOffTypeId().getId())))
                .map(r -> toRequestResponse(r, meta, types))
                .toList();
    }

    @Override
    @Transactional
    public LeaveRequestResponse createRequest(SaveLeaveRequestCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.companyId());
        ensureEmployee(cmd.employeeId());
        TimeOffType type = loadType(cmd.timeOffTypeId());
        if (type.isCompensatory()) throw new HrDomainException("Compensatory time off is not supported");
        BigDecimal days = cmd.numberOfDays() != null
                ? cmd.numberOfDays()
                : LeaveRequest.computeDays(cmd.dateFrom(), cmd.dateTo());
        LeaveRequest request = LeaveRequest.builder()
                .id(new LeaveRequestId(UUID.randomUUID()))
                .companyId(companyId)
                .employeeId(new EmployeeId(cmd.employeeId()))
                .timeOffTypeId(new TimeOffTypeId(cmd.timeOffTypeId()))
                .dateFrom(cmd.dateFrom())
                .dateTo(cmd.dateTo())
                .numberOfDays(days)
                .state(LeaveRequest.STATE_CONFIRM)
                .description(cmd.description())
                .build();
        request.validate();
        LeaveRequest saved = requestRepository.save(request);
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(Set.of(saved.getEmployeeId().getId()));
        Map<UUID, TimeOffType> types = loadTypes(companyId);
        return toRequestResponse(saved, meta, types);
    }

    @Override
    @Transactional
    public LeaveRequestResponse approveRequest(UUID id) {
        LeaveRequest request = loadRequest(id);
        LeaveRequest saved = requestRepository.save(request.approve());
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(Set.of(saved.getEmployeeId().getId()));
        Map<UUID, TimeOffType> types = loadTypes(saved.getCompanyId());
        return toRequestResponse(saved, meta, types);
    }

    @Override
    @Transactional
    public LeaveRequestResponse refuseRequest(UUID id) {
        LeaveRequest request = loadRequest(id);
        LeaveRequest saved = requestRepository.save(request.refuse());
        Map<UUID, EmployeeDisplayMeta> meta = loadMeta(Set.of(saved.getEmployeeId().getId()));
        Map<UUID, TimeOffType> types = loadTypes(saved.getCompanyId());
        return toRequestResponse(saved, meta, types);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(CompanyId companyId, UUID employeeId, int year) {
        UUID empId = resolveDashboardEmployee(companyId, employeeId);
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        Map<UUID, TimeOffType> types = loadTypes(companyId);
        List<TimeOffTypeResponse> typeResponses = types.values().stream()
                .filter(t -> !t.isCompensatory())
                .map(this::toTypeResponse)
                .toList();

        List<DashboardSummaryResponse> summaries = allocationRepository.findApproved(companyId, empId).stream()
                .filter(a -> !isCompensatoryType(types.get(a.getTimeOffTypeId().getId())))
                .map(a -> {
                    TimeOffType type = types.get(a.getTimeOffTypeId().getId());
                    BigDecimal used = requestRepository.sumValidatedDays(
                            companyId, empId, a.getTimeOffTypeId().getId(), a.getDateFrom(), a.getDateTo());
                    BigDecimal available = a.getNumberOfDays().subtract(used);
                    return new DashboardSummaryResponse(
                            a.getTimeOffTypeId().getId(),
                            type != null ? type.getName() : a.getName(),
                            type != null ? type.getColorHex() : "#714B67",
                            a.getNumberOfDays(),
                            used,
                            available.max(BigDecimal.ZERO),
                            a.getDateTo());
                })
                .toList();

        List<LeaveRequest> requests = requestRepository.search(companyId, empId, null, from, to).stream()
                .filter(r -> !isCompensatoryType(types.get(r.getTimeOffTypeId().getId())))
                .toList();

        List<DashboardCalendarDayResponse> calendarDays = new ArrayList<>();
        for (LeaveRequest r : requests) {
            TimeOffType type = types.get(r.getTimeOffTypeId().getId());
            LocalDate d = r.getDateFrom();
            while (!d.isAfter(r.getDateTo())) {
                calendarDays.add(new DashboardCalendarDayResponse(
                        d, "leave", r.getId().getId(), r.getState(),
                        r.getTimeOffTypeId().getId(),
                        type != null ? type.getName() : null,
                        type != null ? type.getColorHex() : "#714B67"));
                d = d.plusDays(1);
            }
        }

        for (var h : holidayRepository.findPublicHolidays(companyId, from, to)) {
            calendarDays.add(new DashboardCalendarDayResponse(
                    h.date(), "public_holiday", null, "validate", null, h.name(), "#E91E63"));
        }
        for (var m : holidayRepository.findMandatoryDays(companyId, from, to)) {
            calendarDays.add(new DashboardCalendarDayResponse(
                    m.date(), "mandatory", null, "validate", null, m.name(), "#FF5722"));
        }

        EmployeeDisplayMeta empMeta = loadMeta(Set.of(empId)).get(empId);
        long pending = requestRepository.countPending(companyId, empId);

        return new DashboardResponse(
                year,
                empId,
                empMeta != null ? empMeta.displayName() : null,
                pending,
                summaries,
                calendarDays,
                holidayRepository.findPublicHolidays(companyId, from, to).stream()
                        .map(h -> new HolidayResponse(h.name(), h.date(), h.countryCode())).toList(),
                holidayRepository.findMandatoryDays(companyId, from, to).stream()
                        .map(m -> new MandatoryDayResponse(m.name(), m.date())).toList(),
                typeResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamSummaryResponse teamSummary(CompanyId companyId, UUID managerId, LocalDate date) {
        LocalDate summaryDate = date != null ? date : LocalDate.now();
        List<com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Employee> employees =
                employeeRepository.findByCompanyId(companyId);
        if (managerId != null) {
            employees = employees.stream()
                    .filter(e -> e.getManagerId() != null && e.getManagerId().getId().equals(managerId))
                    .toList();
        }
        Map<UUID, EmployeeDisplayMeta> meta = employeeRepository.findDisplayMetaByEmployeeIds(
                employees.stream().map(e -> e.getId().getId()).toList());

        List<TeamSummaryItemResponse> members = new ArrayList<>();
        for (var employee : employees) {
            UUID empId = employee.getId().getId();
            EmployeeDisplayMeta empMeta = meta.get(empId);
            boolean outToday = requestRepository.search(
                    companyId, empId, "validate", summaryDate, summaryDate).stream()
                    .anyMatch(r -> !r.getDateFrom().isAfter(summaryDate) && !r.getDateTo().isBefore(summaryDate));
            long pending = requestRepository.countPending(companyId, empId);
            BigDecimal available = allocationRepository.findApproved(companyId, empId).stream()
                    .map(a -> a.getNumberOfDays().subtract(
                            requestRepository.sumValidatedDays(companyId, empId, a.getTimeOffTypeId().getId(),
                                    a.getDateFrom(), a.getDateTo())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            members.add(new TeamSummaryItemResponse(
                    empId,
                    empMeta != null ? empMeta.displayName() : employee.getDisplayName(),
                    empMeta != null ? empMeta.imageUrl() : null,
                    outToday,
                    pending,
                    available.max(BigDecimal.ZERO)));
        }
        return new TeamSummaryResponse(summaryDate, members);
    }

    private UUID resolveDashboardEmployee(CompanyId companyId, UUID employeeId) {
        if (employeeId != null) {
            ensureEmployee(employeeId);
            return employeeId;
        }
        var page = employeeRepository.search(companyId, null, null, false,
                org.springframework.data.domain.PageRequest.of(0, 1));
        if (page.isEmpty()) throw new HrDomainException("No employees found for dashboard");
        return page.getContent().get(0).getId().getId();
    }

    private Map<UUID, TimeOffType> loadTypes(CompanyId companyId) {
        return typeRepository.findByCompany(companyId).stream()
                .collect(Collectors.toMap(t -> t.getId().getId(), t -> t));
    }

    private boolean isCompensatoryType(TimeOffType type) {
        return type != null && type.isCompensatory();
    }

    private Map<UUID, EmployeeDisplayMeta> loadMeta(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return employeeRepository.findDisplayMetaByEmployeeIds(ids);
    }

    private void ensureEmployee(UUID employeeId) {
        employeeRepository.findById(new EmployeeId(employeeId))
                .orElseThrow(() -> new HrDomainException("Employee not found: " + employeeId));
    }

    private TimeOffType loadType(UUID id) {
        return typeRepository.findById(new TimeOffTypeId(id))
                .orElseThrow(() -> new HrDomainException("Time off type not found: " + id));
    }

    private LeaveAllocation loadAllocation(UUID id) {
        return allocationRepository.findById(new LeaveAllocationId(id))
                .orElseThrow(() -> new HrDomainException("Allocation not found: " + id));
    }

    private LeaveRequest loadRequest(UUID id) {
        return requestRepository.findById(new LeaveRequestId(id))
                .orElseThrow(() -> new HrDomainException("Leave request not found: " + id));
    }

    private CompanyId resolveCompany(UUID companyId) {
        if (companyId != null) return new CompanyId(companyId);
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) return ctx.requireCompany();
        throw new HrDomainException("companyId required");
    }

    private TimeOffTypeResponse toTypeResponse(TimeOffType t) {
        return new TimeOffTypeResponse(
                t.getId().getId(), t.getName(), t.getCode(), t.getDisplayCode(),
                t.getCountryCode(), t.getColorHex(), t.getSortOrder(), t.isActive());
    }

    private AllocationResponse toAllocationResponse(LeaveAllocation a,
                                                    Map<UUID, EmployeeDisplayMeta> meta,
                                                    Map<UUID, TimeOffType> types,
                                                    CompanyId companyId) {
        EmployeeDisplayMeta emp = meta.get(a.getEmployeeId().getId());
        TimeOffType type = types.get(a.getTimeOffTypeId().getId());
        BigDecimal used = a.isApproved()
                ? requestRepository.sumValidatedDays(companyId, a.getEmployeeId().getId(),
                a.getTimeOffTypeId().getId(), a.getDateFrom(), a.getDateTo())
                : BigDecimal.ZERO;
        BigDecimal remaining = a.getNumberOfDays().subtract(used);
        return new AllocationResponse(
                a.getId().getId(),
                a.getEmployeeId().getId(),
                emp != null ? emp.displayName() : null,
                emp != null ? emp.imageUrl() : null,
                a.getTimeOffTypeId().getId(),
                type != null ? type.getName() : a.getName(),
                type != null ? type.getColorHex() : "#714B67",
                a.getName(),
                a.getNumberOfDays(),
                used,
                remaining.max(BigDecimal.ZERO),
                a.getAllocationType(),
                a.getState(),
                a.getDateFrom(),
                a.getDateTo());
    }

    private LeaveRequestResponse toRequestResponse(LeaveRequest r,
                                                   Map<UUID, EmployeeDisplayMeta> meta,
                                                   Map<UUID, TimeOffType> types) {
        EmployeeDisplayMeta emp = meta.get(r.getEmployeeId().getId());
        TimeOffType type = types.get(r.getTimeOffTypeId().getId());
        return new LeaveRequestResponse(
                r.getId().getId(),
                r.getEmployeeId().getId(),
                emp != null ? emp.displayName() : null,
                emp != null ? emp.imageUrl() : null,
                r.getTimeOffTypeId().getId(),
                type != null ? type.getName() : null,
                type != null ? type.getColorHex() : "#714B67",
                type != null ? type.getDisplayCode() : null,
                r.getDateFrom(),
                r.getDateTo(),
                r.getNumberOfDays(),
                r.getState(),
                r.getDescription());
    }
}
