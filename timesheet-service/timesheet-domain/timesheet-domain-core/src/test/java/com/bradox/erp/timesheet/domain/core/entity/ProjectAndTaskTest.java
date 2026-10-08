package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectAndTaskTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    private static Project project(BillingMode mode, boolean billable, String systemKey) {
        return Project.create(new ProjectId(UUID.randomUUID()), COMPANY, "Website", " web-1 ", null, null,
                true, mode, billable, 600, null, systemKey, NOW, "tester");
    }

    @Test
    void fixedPriceForcesBillableDefaultOff() {
        assertFalse(project(BillingMode.FIXED_PRICE, true, null).isBillableDefault());
        assertTrue(project(BillingMode.HOURLY, true, null).isBillableDefault());
    }

    @Test
    void archivedProjectTakesNoEntries() {
        Project p = project(BillingMode.HOURLY, false, null);
        assertTrue(p.acceptsEntries());
        p.archive();
        assertEquals(ProjectStatus.ARCHIVED, p.getStatus());
        assertFalse(p.acceptsEntries());
        p.unarchive();
        assertTrue(p.acceptsEntries());
    }

    @Test
    void projectWithTimesheetsOffTakesNoEntries() {
        Project p = project(BillingMode.HOURLY, false, null);
        p.update("Website", "web-1", null, null, false, BillingMode.HOURLY, false, null, null);
        assertFalse(p.acceptsEntries());
    }

    @Test
    void internalProjectCannotBeArchived() {
        assertThrows(TimesheetDomainException.class, () -> project(BillingMode.HOURLY, false, Project.INTERNAL_KEY).archive());
    }

    @Test
    void nameIsRequiredAndCodeNormalizes() {
        assertThrows(TimesheetDomainException.class, () -> Project.create(new ProjectId(UUID.randomUUID()), COMPANY,
                " ", null, null, null, true, BillingMode.HOURLY, false, null, null, null, NOW, "t"));
        assertEquals("WEB-1", Project.normalizeCode(" web-1 "));
        assertEquals(null, Project.normalizeCode("  "));
    }

    @Test
    void taskStatusFlowAndEntryAcceptance() {
        Task t = Task.create(new TaskId(UUID.randomUUID()), COMPANY, new ProjectId(UUID.randomUUID()), "Design", null,
                Set.of(), 120, null, NOW, "t");
        assertEquals(TaskStatus.TODO, t.getStatus());
        assertTrue(t.acceptsEntries());
        t.changeStatus(TaskStatus.IN_PROGRESS, NOW.plusSeconds(60));
        assertTrue(t.acceptsEntries());
        t.changeStatus(TaskStatus.DONE, NOW.plusSeconds(120));
        assertFalse(t.acceptsEntries());
        assertEquals(NOW.plusSeconds(120), t.getStatusChangedAt());
        t.changeStatus(TaskStatus.CANCELED, NOW.plusSeconds(180));
        assertFalse(t.acceptsEntries());
    }

    @Test
    void sameStatusDoesNotTouchTimestamp() {
        Task t = Task.create(new TaskId(UUID.randomUUID()), COMPANY, new ProjectId(UUID.randomUUID()), "Design", null,
                null, null, null, NOW, "t");
        t.changeStatus(TaskStatus.TODO, NOW.plusSeconds(500));
        assertEquals(NOW, t.getStatusChangedAt());
    }

    @Test
    void negativeAllocationRejected() {
        assertThrows(TimesheetDomainException.class, () -> Task.create(new TaskId(UUID.randomUUID()), COMPANY,
                new ProjectId(UUID.randomUUID()), "x", null, null, -1, null, NOW, "t"));
    }
}
