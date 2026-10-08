package com.bradox.erp.timesheet.dataaccess.mapper;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.EntryEntity;
import com.bradox.erp.timesheet.dataaccess.entity.ProjectEntity;
import com.bradox.erp.timesheet.dataaccess.entity.SettingsEntity;
import com.bradox.erp.timesheet.dataaccess.entity.TaskEntity;
import com.bradox.erp.timesheet.dataaccess.entity.WeekEntity;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.TimeFormat;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import org.springframework.stereotype.Component;

import com.bradox.erp.timesheet.dataaccess.entity.TimerEntity;
import com.bradox.erp.timesheet.domain.core.entity.Timer;
import com.bradox.erp.timesheet.domain.core.valueobject.TimerId;

import java.time.DayOfWeek;
import java.util.LinkedHashSet;

@Component
public class TimesheetDataAccessMapper {

    // ---- project

    public Project toDomain(ProjectEntity e) {
        return Project.restore(new ProjectId(e.getId()), new CompanyId(e.getCompanyId()), e.getName(), e.getCode(),
                e.getPartnerId(), e.getManagerEmployeeId(), e.isAllowTimesheets(),
                BillingMode.valueOf(e.getBillingMode()), e.isBillableDefault(), e.getAllocatedMinutes(),
                e.getLinkedModel(), e.getSystemKey(), ProjectStatus.valueOf(e.getStatus()), e.getColor(),
                e.getCreatedAt(), e.getCreatedBy(), e.getDefaultSaleLineId(), e.getCostAccountId());
    }

    public void apply(Project p, ProjectEntity e) {
        e.setId(p.getId().getId());
        e.setCompanyId(p.getCompanyId().getId());
        e.setName(p.getName());
        e.setCode(p.getCode());
        e.setCodeNormalized(Project.normalizeCode(p.getCode()));
        e.setPartnerId(p.getPartnerId());
        e.setManagerEmployeeId(p.getManagerEmployeeId());
        e.setAllowTimesheets(p.isAllowTimesheets());
        e.setBillingMode(p.getBillingMode().name());
        e.setBillableDefault(p.isBillableDefault());
        e.setAllocatedMinutes(p.getAllocatedMinutes());
        e.setLinkedModel(p.getLinkedModel());
        e.setSystemKey(p.getSystemKey());
        e.setStatus(p.getStatus().name());
        e.setColor(p.getColor());
        e.setDefaultSaleLineId(p.getDefaultSaleLineId());
        e.setCostAccountId(p.getCostAccountId());
        e.setCreatedAt(p.getCreatedAt());
        e.setCreatedBy(p.getCreatedBy());
    }

    // ---- task

    public Task toDomain(TaskEntity e) {
        return Task.restore(new TaskId(e.getId()), new CompanyId(e.getCompanyId()), new ProjectId(e.getProjectId()),
                e.getName(), e.getDescription(), TaskStatus.valueOf(e.getStatus()), e.getStatusChangedAt(),
                e.getAssigneeEmployeeIds(), e.getAllocatedMinutes(), e.getDeadline(), e.getRecordModel(),
                e.getRecordId(), e.getCreatedAt(), e.getCreatedBy(), e.getSaleLineId());
    }

    public void apply(Task t, TaskEntity e) {
        e.setId(t.getId().getId());
        e.setCompanyId(t.getCompanyId().getId());
        e.setProjectId(t.getProjectId().getId());
        e.setName(t.getName());
        e.setDescription(t.getDescription());
        e.setStatus(t.getStatus().name());
        e.setStatusChangedAt(t.getStatusChangedAt());
        e.setAllocatedMinutes(t.getAllocatedMinutes());
        e.setDeadline(t.getDeadline());
        e.setRecordModel(t.getRecordModel());
        e.setRecordId(t.getRecordId());
        e.setSaleLineId(t.getSaleLineId());
        e.setCreatedAt(t.getCreatedAt());
        e.setCreatedBy(t.getCreatedBy());
        // Replace contents in place so Hibernate sees a change to the managed collection.
        e.getAssigneeEmployeeIds().retainAll(t.getAssigneeEmployeeIds());
        e.getAssigneeEmployeeIds().addAll(new LinkedHashSet<>(t.getAssigneeEmployeeIds()));
    }

    // ---- entry

    public Entry toDomain(EntryEntity e) {
        return Entry.restore(new EntryId(e.getId()), new CompanyId(e.getCompanyId()), e.getEmployeeId(),
                e.getWorkDate(), e.getMinutes(), new ProjectId(e.getProjectId()),
                e.getTaskId() == null ? null : new TaskId(e.getTaskId()), e.getDescription(), e.isBillable(),
                new WeekId(e.getWeekId()), EntrySource.valueOf(e.getSource()), e.getCreatedAt(), e.getCreatedBy(),
                e.getUpdatedAt(), e.getCostRate(), e.getCostCurrency(), e.getCostAmount(), e.isCostMissing(),
                e.getRecordModel(), e.getRecordId(), e.getSaleLineId());
    }

    public void apply(Entry n, EntryEntity e) {
        e.setId(n.getId().getId());
        e.setCompanyId(n.getCompanyId().getId());
        e.setEmployeeId(n.getEmployeeId());
        e.setWorkDate(n.getWorkDate());
        e.setMinutes(n.getMinutes());
        e.setProjectId(n.getProjectId().getId());
        e.setTaskId(n.getTaskId() == null ? null : n.getTaskId().getId());
        e.setDescription(n.getDescription());
        e.setBillable(n.isBillable());
        e.setWeekId(n.getWeekId().getId());
        e.setSource(n.getSource().name());
        e.setCreatedAt(n.getCreatedAt());
        e.setCreatedBy(n.getCreatedBy());
        e.setUpdatedAt(n.getUpdatedAt());
        e.setCostRate(n.getCostRate());
        e.setCostCurrency(n.getCostCurrency());
        e.setCostAmount(n.getCostAmount());
        e.setCostMissing(n.isCostMissing());
        e.setRecordModel(n.getRecordModel());
        e.setRecordId(n.getRecordId());
        e.setSaleLineId(n.getSaleLineId());
    }

    // ---- week

    public TimesheetWeek toDomain(WeekEntity e) {
        return TimesheetWeek.restore(new WeekId(e.getId()), new CompanyId(e.getCompanyId()), e.getEmployeeId(),
                e.getWeekStart(), WeekStatus.valueOf(e.getStatus()), e.isLocked(), e.getCreatedAt(), e.getSubmittedAt(),
                e.getApprovedBy(), e.getApprovedAt(), e.getRefusedReason());
    }

    /** Copies every field the domain owns, including the submit and approval trail. */
    public void apply(TimesheetWeek w, WeekEntity e) {
        e.setId(w.getId().getId());
        e.setCompanyId(w.getCompanyId().getId());
        e.setEmployeeId(w.getEmployeeId());
        e.setWeekStart(w.getWeekStart());
        e.setStatus(w.getStatus().name());
        e.setLocked(w.isLocked());
        e.setCreatedAt(w.getCreatedAt());
        e.setSubmittedAt(w.getSubmittedAt());
        e.setApprovedBy(w.getApprovedBy());
        e.setApprovedAt(w.getApprovedAt());
        e.setRefusedReason(w.getRefusedReason());
    }

    // ---- timer

    public Timer toDomain(TimerEntity e) {
        return Timer.restore(new TimerId(e.getId()), new CompanyId(e.getCompanyId()), e.getEmployeeId(),
                new ProjectId(e.getProjectId()), e.getTaskId() == null ? null : new TaskId(e.getTaskId()),
                e.getDescription(), e.getStartedAt());
    }

    public void apply(Timer t, TimerEntity e) {
        e.setId(t.getId().getId());
        e.setCompanyId(t.getCompanyId().getId());
        e.setEmployeeId(t.getEmployeeId());
        e.setProjectId(t.getProjectId().getId());
        e.setTaskId(t.getTaskId() == null ? null : t.getTaskId().getId());
        e.setDescription(t.getDescription());
        e.setStartedAt(t.getStartedAt());
    }

    // ---- settings

    public TimesheetSettings toDomain(SettingsEntity e) {
        TimesheetSettings s = TimesheetSettings.withWorkflow(TimesheetSettings.restore(new CompanyId(e.getCompanyId()),
                TimeFormat.valueOf(e.getTimeFormat()), DayOfWeek.valueOf(e.getWeekStartDay()), e.getTimezone(),
                e.isApprovalRequired(), e.getAllowFutureDays(), e.getMinutesPerDay()), e.isManagersMaySelfApprove(),
                e.getAutoLockAfterDays(), e.getRoundingStepMinutes(),
                com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode.valueOf(e.getRoundingMode()));
        s.updateLedger(e.isLedgerPostingEnabled(), e.getDefaultCostAccountId(), e.getLaborAppliedAccountId(), e.getJournalCode());
        s.updateReminder(e.isReminderEnabled(), DayOfWeek.valueOf(e.getReminderWeekday()));
        return s;
    }

    public void apply(TimesheetSettings s, SettingsEntity e) {
        e.setCompanyId(s.getCompanyId().getId());
        e.setTimeFormat(s.getTimeFormat().name());
        e.setWeekStartDay(s.getWeekStartDay().name());
        e.setTimezone(s.getZone().getId());
        e.setApprovalRequired(s.isApprovalRequired());
        e.setAllowFutureDays(s.getAllowFutureDays());
        e.setMinutesPerDay(s.getMinutesPerDay());
        e.setManagersMaySelfApprove(s.isManagersMaySelfApprove());
        e.setAutoLockAfterDays(s.getAutoLockAfterDays());
        e.setRoundingStepMinutes(s.getRoundingStepMinutes());
        e.setRoundingMode(s.getRoundingMode().name());
        e.setLedgerPostingEnabled(s.isLedgerPostingEnabled());
        e.setDefaultCostAccountId(s.getDefaultCostAccountId());
        e.setLaborAppliedAccountId(s.getLaborAppliedAccountId());
        e.setJournalCode(s.getJournalCode());
        e.setReminderEnabled(s.isReminderEnabled());
        e.setReminderWeekday(s.getReminderWeekday().name());
    }
}
