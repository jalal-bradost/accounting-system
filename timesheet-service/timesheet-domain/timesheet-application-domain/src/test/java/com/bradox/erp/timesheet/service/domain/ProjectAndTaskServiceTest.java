package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import com.bradox.erp.timesheet.service.domain.dto.ProjectCommand;
import com.bradox.erp.timesheet.service.domain.dto.ProjectResponse;
import com.bradox.erp.timesheet.service.domain.dto.TaskCommand;
import com.bradox.erp.timesheet.service.domain.dto.TaskResponse;
import com.bradox.erp.timesheet.service.domain.dto.TaskStatusCommand;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectAndTaskServiceTest extends ServiceTestBase {

    private static ProjectCommand cmd(String name, String code, BillingMode mode, Boolean billable) {
        return new ProjectCommand(name, code, null, null, true, mode, billable, 600, null);
    }

    @Test
    void creatingProjectsNeedsThePermission() {
        assertThrows(ForbiddenException.class, () -> projectService.create(COMPANY, cmd("X", null, BillingMode.HOURLY, false)));
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        ProjectResponse r = projectService.create(COMPANY, cmd("Website", "WEB", BillingMode.HOURLY, true));
        assertEquals(ProjectStatus.ACTIVE, r.status());
        assertTrue(r.billableDefault());
    }

    @Test
    void fixedPriceLocksBillableDefaultOff() {
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        assertFalse(projectService.create(COMPANY, cmd("Job", null, BillingMode.FIXED_PRICE, true)).billableDefault());
    }

    @Test
    void codeIsUniquePerCompanyIgnoringCase() {
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        ProjectResponse a = projectService.create(COMPANY, cmd("A", "web", BillingMode.HOURLY, false));
        assertEquals("error.timesheet.projectCodeTaken", assertThrows(TimesheetDomainException.class,
                () -> projectService.create(COMPANY, cmd("B", "WEB", BillingMode.HOURLY, false))).getMessageKey());
        // the project may keep its own code on update
        projectService.update(COMPANY, a.id(), cmd("A renamed", "WEB", BillingMode.HOURLY, false));
    }

    @Test
    void unknownCustomerOrManagerIsRejected() {
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        assertThrows(TimesheetDomainException.class, () -> projectService.create(COMPANY,
                new ProjectCommand("X", null, UUID.randomUUID(), null, true, BillingMode.HOURLY, false, null, null)));
        assertThrows(TimesheetDomainException.class, () -> projectService.create(COMPANY,
                new ProjectCommand("X", null, null, UUID.randomUUID(), true, BillingMode.HOURLY, false, null, null)));
    }

    @Test
    void internalProjectIsSeededOnceAndCannotBeArchived() {
        var first = projectService.list(COMPANY, false);
        assertEquals(1, first.size());
        assertEquals("Internal", first.get(0).name());
        assertTrue(first.get(0).internal());
        assertFalse(first.get(0).billableDefault());
        assertEquals(1, projectService.list(COMPANY, false).size(), "seeding is idempotent");
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        assertThrows(TimesheetDomainException.class, () -> projectService.archive(COMPANY, first.get(0).id()));
    }

    @Test
    void archivedProjectLeavesPickersAndKeepsItsHours() {
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        ProjectResponse p = projectService.create(COMPANY, cmd("Website", null, BillingMode.HOURLY, false));
        entryService.log(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.EntryCommand(null, TODAY, p.id(), null,
                90, null, null, null));
        projectService.archive(COMPANY, p.id());
        assertFalse(projectService.list(COMPANY, false).stream().anyMatch(x -> x.id().equals(p.id())));
        ProjectResponse archived = projectService.list(COMPANY, true).stream().filter(x -> x.id().equals(p.id())).findFirst().orElseThrow();
        assertEquals(90, archived.loggedMinutes());
        projectService.unarchive(COMPANY, p.id());
        assertTrue(projectService.list(COMPANY, false).stream().anyMatch(x -> x.id().equals(p.id())));
    }

    @Test
    void tasksStartTodoMoveThroughStatusesAndReportLoggedTime() {
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        permissions.add(TimesheetPermissions.TASK_MANAGE);
        ProjectResponse p = projectService.create(COMPANY, cmd("Website", null, BillingMode.HOURLY, false));
        var emp = employees.add("Beth Evans", null);
        TaskResponse t = taskService.create(COMPANY, new TaskCommand(p.id(), "Design", null, Set.of(emp.id()), 120, null));
        assertEquals(TaskStatus.TODO, t.status());
        assertEquals(java.util.List.of("Beth Evans"), t.assigneeNames());
        entryService.log(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.EntryCommand(null, TODAY, null, t.id(),
                30, null, null, null));
        assertEquals(30, taskService.listByProject(COMPANY, p.id()).get(0).loggedMinutes());
        TaskResponse done = taskService.changeStatus(COMPANY, t.id(), new TaskStatusCommand(TaskStatus.DONE));
        assertFalse(done.acceptsEntries());
        assertTrue(taskService.listOpen(COMPANY).isEmpty());
        assertTrue(taskService.changeStatus(COMPANY, t.id(), new TaskStatusCommand(TaskStatus.IN_PROGRESS)).acceptsEntries());
    }

    @Test
    void taskCannotBeAddedWithoutPermissionOrToAnArchivedProject() {
        permissions.add(TimesheetPermissions.PROJECT_MANAGE);
        ProjectResponse p = projectService.create(COMPANY, cmd("Website", null, BillingMode.HOURLY, false));
        assertThrows(ForbiddenException.class,
                () -> taskService.create(COMPANY, new TaskCommand(p.id(), "T", null, null, null, null)));
        permissions.add(TimesheetPermissions.TASK_MANAGE);
        projectService.archive(COMPANY, p.id());
        assertThrows(TimesheetDomainException.class,
                () -> taskService.create(COMPANY, new TaskCommand(p.id(), "T", null, null, null, null)));
    }

    @Test
    void settingsDefaultToSaturdayAndBaghdad() {
        var s = settingsService.get(COMPANY);
        assertEquals(java.time.DayOfWeek.SATURDAY, s.weekStartDay());
        assertEquals("Asia/Baghdad", s.timezone());
        assertThrows(ForbiddenException.class, () -> settingsService.update(COMPANY,
                new com.bradox.erp.timesheet.service.domain.dto.SettingsCommand(null, java.time.DayOfWeek.SUNDAY, null, null, null, null)));
        permissions.add(TimesheetPermissions.SETTINGS_MANAGE);
        assertEquals(java.time.DayOfWeek.SUNDAY, settingsService.update(COMPANY,
                new com.bradox.erp.timesheet.service.domain.dto.SettingsCommand(null, java.time.DayOfWeek.SUNDAY, null, null, null, null)).weekStartDay());
        assertThrows(TimesheetDomainException.class, () -> settingsService.update(COMPANY,
                new com.bradox.erp.timesheet.service.domain.dto.SettingsCommand(null, null, "Nowhere/Land", null, null, null)));
    }
}
