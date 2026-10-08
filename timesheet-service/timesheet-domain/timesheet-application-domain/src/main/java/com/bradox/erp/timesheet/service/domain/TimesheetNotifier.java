package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.TimesheetNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The in-app notifications of the approval flow (TSH-05 #1, #5). Every call is best effort: a notification that cannot
 * be delivered is logged and dropped, never allowed to undo a submit, approval or refusal.
 */
@Component
class TimesheetNotifier {

    static final String WEEK_MODEL = "tsh.week";
    private static final Logger log = LoggerFactory.getLogger(TimesheetNotifier.class);
    private static final int MAX_FALLBACK_APPROVERS = 10;

    private final TimesheetNotificationPort port;
    private final EmployeeLookupPort employees;
    private final TimesheetAccess access;

    TimesheetNotifier(TimesheetNotificationPort port, EmployeeLookupPort employees, TimesheetAccess access) {
        this.port = port;
        this.employees = employees;
        this.access = access;
    }

    /** TSH-05 #1: the approver gets a to-do. HR manager first, then department manager, then Timesheet Managers (D4). */
    void weekSubmitted(CompanyId companyId, TimesheetWeek week, int totalMinutes) {
        attempt("submit notification", () -> {
            // The employee fixed and resubmitted: the old "refused" or "reopened" notice has done its job.
            port.completeTodos(companyId, WEEK_MODEL, week.getId().getId());
            EmployeeRef owner = employees.find(companyId, week.getEmployeeId()).orElse(null);
            if (owner == null) {
                return;
            }
            Set<String> assignees = new LinkedHashSet<>();
            addUser(companyId, owner.managerId(), assignees);
            if (assignees.isEmpty() && owner.departmentId() != null) {
                employees.departmentManager(companyId, owner.departmentId()).ifPresent(m -> addUser(companyId, m, assignees));
            }
            if (assignees.isEmpty()) {
                port.approverAssignees(companyId).stream().limit(MAX_FALLBACK_APPROVERS).forEach(assignees::add);
            }
            String ownerUser = userOf(companyId, owner.id());
            assignees.remove(ownerUser);   // nobody is asked to approve their own week
            String subject = "Timesheet to approve: " + owner.name() + ", week of " + week.getWeekStart();
            String body = owner.name() + " submitted " + hours(totalMinutes) + " for the week of " + week.getWeekStart()
                    + ". Open Timesheets, Approvals to approve or refuse it.";
            for (String a : assignees) {
                port.assignTodo(companyId, WEEK_MODEL, week.getId().getId(), a, subject, body, access.today(access.settings(companyId)));
            }
        });
    }

    /** The approval decision closes the approver's to-do. */
    void approvalDecided(CompanyId companyId, TimesheetWeek week) {
        attempt("complete approval to-dos", () -> port.completeTodos(companyId, WEEK_MODEL, week.getId().getId()));
    }

    /** The employee is told, without a to-do to action, that the week was approved. */
    void weekApproved(CompanyId companyId, TimesheetWeek week) {
        attempt("approval notice", () -> {
            String user = userOf(companyId, week.getEmployeeId());
            if (user != null) {
                port.inform(companyId, WEEK_MODEL, week.getId().getId(), user, "Timesheet approved: week of " + week.getWeekStart(),
                        "Your timesheet for the week of " + week.getWeekStart() + " was approved by " + week.getApprovedBy() + ".");
            }
        });
    }

    /** TSH-05 #5: the employee is told why the week was refused and can fix it. */
    void weekRefused(CompanyId companyId, TimesheetWeek week, String reason) {
        notifyOwner(companyId, week, "Timesheet refused: week of " + week.getWeekStart(),
                "Your timesheet for the week of " + week.getWeekStart() + " was refused. Reason: " + reason
                        + ". Fix it and submit the week again.");
    }

    void weekReopened(CompanyId companyId, TimesheetWeek week, String reason) {
        notifyOwner(companyId, week, "Timesheet reopened: week of " + week.getWeekStart(),
                "Your approved timesheet for the week of " + week.getWeekStart() + " was reopened for changes. Reason: " + reason);
    }

    private void notifyOwner(CompanyId companyId, TimesheetWeek week, String subject, String body) {
        attempt("owner notification", () -> {
            String user = userOf(companyId, week.getEmployeeId());
            if (user != null) {
                port.assignTodo(companyId, WEEK_MODEL, week.getId().getId(), user, subject, body, access.today(access.settings(companyId)));
            }
        });
    }

    private void addUser(CompanyId companyId, UUID employeeId, Set<String> into) {
        if (employeeId == null) {
            return;
        }
        String user = userOf(companyId, employeeId);
        if (user != null) {
            into.add(user);
        }
    }

    private String userOf(CompanyId companyId, UUID employeeId) {
        UUID user = employees.userIds(companyId, java.util.List.of(employeeId)).get(employeeId);
        return user == null ? null : user.toString();
    }

    private static String hours(int minutes) {
        return minutes % 60 == 0 ? (minutes / 60) + "h" : (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private void attempt(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Timesheet {} failed: {}", what, ex.getMessage());
        }
    }
}
