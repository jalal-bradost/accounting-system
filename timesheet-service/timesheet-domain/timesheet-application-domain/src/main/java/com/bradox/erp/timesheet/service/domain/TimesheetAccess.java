package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.platform.web.CompanyContext;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.ApproverRule;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.SettingsRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Who is acting, which employee they act as (D12), and what they may do to someone else's time
 * (BR-TSH-06, BR-TSH-10). Also the one place that knows the company settings and "today".
 */
@Component
class TimesheetAccess {

    /** The employee a request works on, and whether it is a manager acting for someone else. */
    record Target(EmployeeRef employee, boolean onBehalf) {
    }

    private final CompanyContext context;
    private final AuthorizationPort authorization;
    private final EmployeeLookupPort employees;
    private final SettingsRepository settings;
    private final Clock clock;

    @Autowired
    TimesheetAccess(CompanyContext context, AuthorizationPort authorization, EmployeeLookupPort employees,
                    SettingsRepository settings, ObjectProvider<Clock> clockProvider) {
        this(context, authorization, employees, settings, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    TimesheetAccess(CompanyContext context, AuthorizationPort authorization, EmployeeLookupPort employees,
                    SettingsRepository settings, Clock clock) {
        this.context = context;
        this.authorization = authorization;
        this.employees = employees;
        this.settings = settings;
        this.clock = clock;
    }

    boolean can(String permission) {
        UserId user = context.currentUser().orElse(null);
        return authorization.hasAll(user, Set.of(permission));
    }

    void require(String permission) {
        if (!can(permission)) {
            throw new ForbiddenException("error.timesheet.forbidden", new Object[]{permission},
                    "You do not have permission to do that");
        }
    }

    String actorLabel() {
        return context.currentUserDisplay();
    }

    Optional<EmployeeRef> actor(CompanyId companyId) {
        return context.currentUser().flatMap(u -> employees.findByUser(companyId, u.getId()));
    }

    EmployeeRef requireActor(CompanyId companyId) {
        return actor(companyId).orElseThrow(() -> new TimesheetDomainException("error.timesheet.noEmployee", null,
                "Your user is not linked to an employee, so you cannot log time. Ask HR to link it."));
    }

    /** Writing: yourself, or someone else with {@code tsh.entry.on_behalf}. */
    Target resolveForWrite(CompanyId companyId, UUID requestedEmployeeId) {
        Optional<EmployeeRef> me = actor(companyId);
        if (requestedEmployeeId == null || me.map(e -> e.id().equals(requestedEmployeeId)).orElse(false)) {
            return new Target(me.orElseThrow(() -> new TimesheetDomainException("error.timesheet.noEmployee", null,
                    "Your user is not linked to an employee, so you cannot log time. Ask HR to link it.")), false);
        }
        require(TimesheetPermissions.ENTRY_ON_BEHALF);
        return new Target(employees.find(companyId, requestedEmployeeId)
                .orElseThrow(() -> new TimesheetDomainException("error.timesheet.employeeNotFound", null,
                        "Employee not found")), true);
    }

    /** Reading: yourself, or someone else with {@code tsh.entry.view_all}. */
    EmployeeRef resolveForRead(CompanyId companyId, UUID requestedEmployeeId) {
        Optional<EmployeeRef> me = actor(companyId);
        if (requestedEmployeeId == null || me.map(e -> e.id().equals(requestedEmployeeId)).orElse(false)) {
            return me.orElseThrow(() -> new TimesheetDomainException("error.timesheet.noEmployee", null,
                    "Your user is not linked to an employee, so you have no timesheet. Ask HR to link it."));
        }
        if (!can(TimesheetPermissions.ENTRY_VIEW_ALL)) {
            if (!(can(TimesheetPermissions.ENTRY_VIEW_TEAM) && teamIds(companyId).contains(requestedEmployeeId))) {
                require(TimesheetPermissions.ENTRY_VIEW_ALL);
            }
        }
        return employees.find(companyId, requestedEmployeeId)
                .orElseThrow(() -> new TimesheetDomainException("error.timesheet.employeeNotFound", null,
                        "Employee not found"));
    }

    /** Employees who report to the caller, by HR manager or by department manager. */
    Set<UUID> teamIds(CompanyId companyId) {
        return actor(companyId).map(a -> employees.findTeam(companyId, a.id()).stream()
                .map(EmployeeRef::id).collect(Collectors.toSet())).orElse(Set.of());
    }

    boolean canApprove(CompanyId companyId, EmployeeRef owner, TimesheetSettings s) {
        UUID actorId = actor(companyId).map(EmployeeRef::id).orElse(null);
        UUID deptManager = owner.departmentId() == null ? null
                : employees.departmentManager(companyId, owner.departmentId()).orElse(null);
        return ApproverRule.canApprove(actorId, owner.id(), owner.managerId(), deptManager,
                can(TimesheetPermissions.APPROVE), can(TimesheetPermissions.APPROVE_ALL), s.isManagersMaySelfApprove());
    }

    enum Scope { OWN, TEAM, ALL }

    /** TSH-08 #4: Employee sees own numbers, Team Approver their team, Manager and Accountant everyone. */
    Scope reportScope(CompanyId companyId) {
        if (can(TimesheetPermissions.ENTRY_VIEW_ALL) || can(TimesheetPermissions.REPORT_VIEW)) {
            return Scope.ALL;
        }
        if ((can(TimesheetPermissions.ENTRY_VIEW_TEAM) || can(TimesheetPermissions.APPROVE))
                && !teamIds(companyId).isEmpty()) {
            return Scope.TEAM;
        }
        return Scope.OWN;
    }

    /** May the caller change this employee's entries? Own entries, or on-behalf. */
    boolean canModifyFor(CompanyId companyId, UUID entryEmployeeId) {
        boolean mine = actor(companyId).map(e -> e.id().equals(entryEmployeeId)).orElse(false);
        return mine ? can(TimesheetPermissions.ENTRY_OWN) || can(TimesheetPermissions.ENTRY_ON_BEHALF)
                : can(TimesheetPermissions.ENTRY_ON_BEHALF);
    }

    Target requireModifiable(CompanyId companyId, UUID entryEmployeeId) {
        if (!canModifyFor(companyId, entryEmployeeId)) {
            throw new ForbiddenException("error.timesheet.notYourEntry", null,
                    "You can only change your own time entries");
        }
        boolean mine = actor(companyId).map(e -> e.id().equals(entryEmployeeId)).orElse(false);
        EmployeeRef emp = mine ? actor(companyId).get() : employees.find(companyId, entryEmployeeId)
                .orElseThrow(() -> new TimesheetDomainException("error.timesheet.employeeNotFound", null,
                        "Employee not found"));
        return new Target(emp, !mine);
    }

    TimesheetSettings settings(CompanyId companyId) {
        return settings.find(companyId).orElseGet(() -> TimesheetSettings.defaults(companyId));
    }

    LocalDate today(TimesheetSettings s) {
        return LocalDate.now(clock.withZone(s.getZone()));
    }

    Clock clock() {
        return clock;
    }
}
