package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.service.domain.dto.EmployeeRefResponse;
import com.bradox.erp.timesheet.service.domain.dto.MeResponse;
import com.bradox.erp.timesheet.service.domain.dto.SettingsCommand;
import com.bradox.erp.timesheet.service.domain.dto.SettingsResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.SettingsApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPostingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class SettingsApplicationServiceImpl implements SettingsApplicationService {

    static final String AUDIT_MODEL = "tsh.settings";

    private final SettingsRepository settings;
    private final EmployeeLookupPort employees;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final LaborCostPostingPort ledger;

    SettingsApplicationServiceImpl(SettingsRepository settings, EmployeeLookupPort employees, AuditLogPort audit,
                                   TimesheetAccess access, LaborCostPostingPort ledger) {
        this.settings = settings;
        this.employees = employees;
        this.audit = audit;
        this.access = access;
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsResponse get(CompanyId companyId) {
        return toResponse(access.settings(companyId));
    }

    @Override
    @Transactional
    public SettingsResponse update(CompanyId companyId, SettingsCommand c) {
        access.require(TimesheetPermissions.SETTINGS_MANAGE);
        TimesheetSettings s = access.settings(companyId);
        s.update(c.timeFormat(), c.weekStartDay(), c.timezone(),
                c.approvalRequired() == null ? s.isApprovalRequired() : c.approvalRequired(),
                c.allowFutureDays() == null ? s.getAllowFutureDays() : c.allowFutureDays(),
                c.minutesPerDay() == null ? s.getMinutesPerDay() : c.minutesPerDay());
        s.updateWorkflow(
                c.managersMaySelfApprove() == null ? s.isManagersMaySelfApprove() : c.managersMaySelfApprove(),
                Boolean.TRUE.equals(c.clearAutoLock()) ? null
                        : c.autoLockAfterDays() != null ? c.autoLockAfterDays() : s.getAutoLockAfterDays(),
                c.roundingStepMinutes() == null ? s.getRoundingStepMinutes() : c.roundingStepMinutes(),
                c.roundingMode() == null ? s.getRoundingMode() : c.roundingMode());
        applyLedger(companyId, s, c);
        if (c.reminderEnabled() != null || c.reminderWeekday() != null) {
            s.updateReminder(c.reminderEnabled() == null ? s.isReminderEnabled() : c.reminderEnabled(), c.reminderWeekday());
        }
        TimesheetSettings saved = settings.save(s);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, companyId.getId(), "Timesheet settings updated",
                Map.of("weekStartDay", saved.getWeekStartDay().name(), "timeFormat", saved.getTimeFormat().name(),
                        "timezone", saved.getZone().getId()));
        return toResponse(saved);
    }

    /**
     * TSH-10: ledger options need {@code tsh.posting.manage}. Turning posting on without accounts provisions the
     * "TSH" journal and the two labor accounts, which the accountant then confirms.
     */
    private void applyLedger(CompanyId companyId, TimesheetSettings s, SettingsCommand c) {
        boolean touched = c.ledgerPostingEnabled() != null || c.defaultCostAccountId() != null
                || c.laborAppliedAccountId() != null || c.journalCode() != null;
        if (!touched) {
            return;
        }
        access.require(TimesheetPermissions.POSTING_MANAGE);
        boolean enabled = c.ledgerPostingEnabled() == null ? s.isLedgerPostingEnabled() : c.ledgerPostingEnabled();
        java.util.UUID cost = c.defaultCostAccountId() != null ? c.defaultCostAccountId() : s.getDefaultCostAccountId();
        java.util.UUID applied = c.laborAppliedAccountId() != null ? c.laborAppliedAccountId() : s.getLaborAppliedAccountId();
        String journal = c.journalCode() != null && !c.journalCode().isBlank() ? c.journalCode() : s.getJournalCode();
        if (enabled && (cost == null || applied == null)) {
            LaborCostPostingPort.Defaults d = ledger.ensureDefaults(companyId, journal);
            cost = cost == null ? d.costAccountId() : cost;
            applied = applied == null ? d.laborAppliedAccountId() : applied;
        }
        s.updateLedger(enabled, cost, applied, journal);
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse me(CompanyId companyId) {
        TimesheetSettings s = access.settings(companyId);
        EmployeeRefResponse employee = access.actor(companyId)
                .map(e -> new EmployeeRefResponse(e.id(), e.name())).orElse(null);
        return new MeResponse(employee, toResponse(s), access.today(s),
                access.can(TimesheetPermissions.ENTRY_ON_BEHALF), access.can(TimesheetPermissions.ENTRY_VIEW_ALL),
                access.can(TimesheetPermissions.PROJECT_MANAGE),
                access.can(TimesheetPermissions.APPROVE) || access.can(TimesheetPermissions.APPROVE_ALL),
                access.can(TimesheetPermissions.ENTRY_VIEW_TEAM) || access.can(TimesheetPermissions.ENTRY_VIEW_ALL),
                access.can(TimesheetPermissions.COST_VIEW),
                access.can(TimesheetPermissions.REPORT_VIEW) || access.can(TimesheetPermissions.ENTRY_VIEW_ALL)
                        || access.can(TimesheetPermissions.ENTRY_VIEW_TEAM),
                access.can(TimesheetPermissions.SETTINGS_MANAGE), access.can(TimesheetPermissions.BILLING_MANAGE),
                access.can(TimesheetPermissions.POSTING_MANAGE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeRefResponse> employees(CompanyId companyId) {
        return employees.listActive(companyId).stream()
                .sorted(Comparator.comparing(e -> e.name().toLowerCase()))
                .map(e -> new EmployeeRefResponse(e.id(), e.name())).toList();
    }

    private static SettingsResponse toResponse(TimesheetSettings s) {
        return new SettingsResponse(s.getTimeFormat(), s.getWeekStartDay(), s.getZone().getId(),
                s.isApprovalRequired(), s.getAllowFutureDays(), s.getMinutesPerDay(), s.isManagersMaySelfApprove(),
                s.getAutoLockAfterDays(), s.getRoundingStepMinutes(), s.getRoundingMode(), s.isLedgerPostingEnabled(),
                s.getDefaultCostAccountId(), s.getLaborAppliedAccountId(), s.getJournalCode(), s.isReminderEnabled(),
                s.getReminderWeekday());
    }
}
