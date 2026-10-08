package com.bradox.erp.integration;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.activity.ActivityApplicationService;
import com.bradox.erp.platform.activity.ActivityKind;
import com.bradox.erp.platform.activity.ActivityResponse;
import com.bradox.erp.platform.activity.CreateActivityCommand;
import com.bradox.erp.timesheet.service.domain.ports.output.TimesheetNotificationPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Delivers timesheet notifications as to-dos in the platform activity inbox (TSH-05). */
@Component
public class TimesheetNotificationAdapter implements TimesheetNotificationPort {

    private static final String APPROVER_LOOKUP_MODEL = "tsh.approval";

    private final ActivityApplicationService activities;

    public TimesheetNotificationAdapter(ActivityApplicationService activities) {
        this.activities = activities;
    }

    @Override
    public void assignTodo(CompanyId companyId, String modelName, UUID recordId, String assignee, String subject, String body,
                           LocalDate dueDate) {
        CreateActivityCommand cmd = new CreateActivityCommand();
        cmd.setCompanyId(companyId.getId());
        cmd.setModelName(modelName);
        cmd.setRecordId(recordId);
        cmd.setKind(ActivityKind.ACTIVITY_TODO);
        cmd.setSubject(subject.length() > 200 ? subject.substring(0, 197) + "…" : subject);
        cmd.setBody(body);
        cmd.setAssigneeId(assignee);
        cmd.setDueDate(dueDate);
        activities.create(cmd);
    }

    @Override
    public void inform(CompanyId companyId, String modelName, UUID recordId, String assignee, String subject, String body) {
        CreateActivityCommand cmd = new CreateActivityCommand();
        cmd.setCompanyId(companyId.getId());
        cmd.setModelName(modelName);
        cmd.setRecordId(recordId);
        cmd.setKind(ActivityKind.ACTIVITY_TODO);
        cmd.setSubject(subject.length() > 200 ? subject.substring(0, 197) + "…" : subject);
        cmd.setBody(body);
        cmd.setAssigneeId(assignee);
        // A note, not a task: created and closed in one step so it sits in the inbox history without nagging.
        activities.complete(activities.create(cmd).getId());
    }

    @Override
    public void completeTodos(CompanyId companyId, String modelName, UUID recordId) {
        for (ActivityResponse a : activities.feed(companyId.getId(), modelName, recordId, PageRequest.of(0, 100))) {
            if (a.getKind() == ActivityKind.ACTIVITY_TODO && a.getCompletedAt() == null) {
                activities.complete(a.getId());
            }
        }
    }

    @Override
    public List<String> approverAssignees(CompanyId companyId) {
        return activities.listAssignees(companyId.getId(), APPROVER_LOOKUP_MODEL).stream().map(a -> a.id()).toList();
    }
}
