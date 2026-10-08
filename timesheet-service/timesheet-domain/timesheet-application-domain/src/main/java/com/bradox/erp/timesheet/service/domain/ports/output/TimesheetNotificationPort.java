package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * In-app notifications through the platform activity inbox (TSH-05 #1, #5, #8). Implemented in {@code infrastructure}.
 * Callers treat every method as best effort: a notification must never break approval or a job.
 */
public interface TimesheetNotificationPort {

    /** Gives {@code assignee} (a username or user id) a to-do on a record; it shows in their activities inbox. */
    void assignTodo(CompanyId companyId, String modelName, UUID recordId, String assignee, String subject, String body,
                    LocalDate dueDate);

    /** Leaves a note in the assignee's inbox history that needs no action: created already done. */
    void inform(CompanyId companyId, String modelName, UUID recordId, String assignee, String subject, String body);

    /** Marks the open to-dos on a record as done, for example once a week has been approved. */
    void completeTodos(CompanyId companyId, String modelName, UUID recordId);

    /** Users who hold {@code tsh.approve_all}: the last resort when an employee has no manager (D4). */
    List<String> approverAssignees(CompanyId companyId);
}
