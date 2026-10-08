package com.bradox.erp.timesheet.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "tsh_company_settings")
public class SettingsEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "time_format", nullable = false, length = 16)
    private String timeFormat;
    @Column(name = "week_start_day", nullable = false, length = 16)
    private String weekStartDay;
    @Column(nullable = false, length = 64)
    private String timezone;
    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;
    @Column(name = "allow_future_days", nullable = false)
    private int allowFutureDays;
    @Column(name = "minutes_per_day", nullable = false)
    private int minutesPerDay;
    @Column(name = "managers_may_self_approve", nullable = false)
    private boolean managersMaySelfApprove;
    @Column(name = "auto_lock_after_days")
    private Integer autoLockAfterDays;
    @Column(name = "rounding_step_minutes", nullable = false)
    private int roundingStepMinutes;
    @Column(name = "rounding_mode", nullable = false, length = 16)
    private String roundingMode = "NEAREST";
    @Column(name = "ledger_posting_enabled", nullable = false)
    private boolean ledgerPostingEnabled;
    @Column(name = "default_cost_account_id")
    private UUID defaultCostAccountId;
    @Column(name = "labor_applied_account_id")
    private UUID laborAppliedAccountId;
    @Column(name = "journal_code", nullable = false, length = 10)
    private String journalCode = "TSH";
    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = true;
    @Column(name = "reminder_weekday", nullable = false, length = 16)
    private String reminderWeekday = "SUNDAY";

    public SettingsEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }
    public String getWeekStartDay() { return weekStartDay; }
    public void setWeekStartDay(String weekStartDay) { this.weekStartDay = weekStartDay; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
    public int getAllowFutureDays() { return allowFutureDays; }
    public void setAllowFutureDays(int allowFutureDays) { this.allowFutureDays = allowFutureDays; }
    public boolean isManagersMaySelfApprove() { return managersMaySelfApprove; }
    public void setManagersMaySelfApprove(boolean v) { this.managersMaySelfApprove = v; }
    public Integer getAutoLockAfterDays() { return autoLockAfterDays; }
    public void setAutoLockAfterDays(Integer v) { this.autoLockAfterDays = v; }
    public int getRoundingStepMinutes() { return roundingStepMinutes; }
    public void setRoundingStepMinutes(int v) { this.roundingStepMinutes = v; }
    public String getRoundingMode() { return roundingMode; }
    public void setRoundingMode(String v) { this.roundingMode = v; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public void setReminderEnabled(boolean v) { this.reminderEnabled = v; }
    public String getReminderWeekday() { return reminderWeekday; }
    public void setReminderWeekday(String v) { this.reminderWeekday = v; }
    public boolean isLedgerPostingEnabled() { return ledgerPostingEnabled; }
    public void setLedgerPostingEnabled(boolean v) { this.ledgerPostingEnabled = v; }
    public UUID getDefaultCostAccountId() { return defaultCostAccountId; }
    public void setDefaultCostAccountId(UUID v) { this.defaultCostAccountId = v; }
    public UUID getLaborAppliedAccountId() { return laborAppliedAccountId; }
    public void setLaborAppliedAccountId(UUID v) { this.laborAppliedAccountId = v; }
    public String getJournalCode() { return journalCode; }
    public void setJournalCode(String v) { this.journalCode = v; }
    public int getMinutesPerDay() { return minutesPerDay; }
    public void setMinutesPerDay(int minutesPerDay) { this.minutesPerDay = minutesPerDay; }
}
