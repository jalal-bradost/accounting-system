package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.TimeFormat;

import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.ZoneId;

/** Per-company timesheet settings. Defaults follow D8 (week starts Saturday) and Iraq's timezone. */
public class TimesheetSettings {

    public static final String DEFAULT_ZONE = "Asia/Baghdad";

    private final CompanyId companyId;
    private TimeFormat timeFormat;
    private DayOfWeek weekStartDay;
    private ZoneId zone;
    private boolean approvalRequired;
    private int allowFutureDays;
    private int minutesPerDay;
    private boolean managersMaySelfApprove;
    private Integer autoLockAfterDays;
    private int roundingStepMinutes;
    private RoundingMode roundingMode = RoundingMode.NEAREST;
    private boolean ledgerPostingEnabled;
    private java.util.UUID defaultCostAccountId;
    private java.util.UUID laborAppliedAccountId;
    private String journalCode = "TSH";
    private boolean reminderEnabled = true;
    private DayOfWeek reminderWeekday = DayOfWeek.SUNDAY;

    private TimesheetSettings(CompanyId companyId, TimeFormat timeFormat, DayOfWeek weekStartDay, ZoneId zone,
                              boolean approvalRequired, int allowFutureDays, int minutesPerDay) {
        this.companyId = companyId;
        this.timeFormat = timeFormat;
        this.weekStartDay = weekStartDay;
        this.zone = zone;
        this.approvalRequired = approvalRequired;
        this.allowFutureDays = allowFutureDays;
        this.minutesPerDay = minutesPerDay;
    }

    public static TimesheetSettings defaults(CompanyId companyId) {
        return new TimesheetSettings(companyId, TimeFormat.HH_MM, DayOfWeek.SATURDAY, ZoneId.of(DEFAULT_ZONE),
                true, 0, 480);
    }

    public static TimesheetSettings restore(CompanyId companyId, TimeFormat timeFormat, DayOfWeek weekStartDay,
                                            String zone, boolean approvalRequired, int allowFutureDays,
                                            int minutesPerDay) {
        return new TimesheetSettings(companyId, timeFormat, weekStartDay, parseZone(zone), approvalRequired,
                allowFutureDays, minutesPerDay);
    }

    public void update(TimeFormat timeFormat, DayOfWeek weekStartDay, String zone, boolean approvalRequired,
                       int allowFutureDays, int minutesPerDay) {
        if (allowFutureDays < 0 || allowFutureDays > 365) {
            throw new TimesheetDomainException("error.timesheet.futureDaysRange", null,
                    "Future days must be between 0 and 365");
        }
        if (minutesPerDay < 60 || minutesPerDay > 1440) {
            throw new TimesheetDomainException("error.timesheet.minutesPerDayRange", null,
                    "Minutes per day must be between 60 and 1440");
        }
        this.timeFormat = timeFormat == null ? this.timeFormat : timeFormat;
        this.weekStartDay = weekStartDay == null ? this.weekStartDay : weekStartDay;
        this.zone = zone == null || zone.isBlank() ? this.zone : parseZone(zone);
        this.approvalRequired = approvalRequired;
        this.allowFutureDays = allowFutureDays;
        this.minutesPerDay = minutesPerDay;
    }

    private static ZoneId parseZone(String zone) {
        try {
            return ZoneId.of(zone);
        } catch (DateTimeException ex) {
            throw new TimesheetDomainException("error.timesheet.zoneInvalid", new Object[]{zone},
                    "Unknown timezone " + zone);
        }
    }

    /** TSH-05 and TSH-04 options, kept apart so the base settings update stays small. */
    public void updateWorkflow(boolean managersMaySelfApprove, Integer autoLockAfterDays, int roundingStepMinutes,
                               RoundingMode roundingMode) {
        if (autoLockAfterDays != null && autoLockAfterDays < 1) {
            throw new TimesheetDomainException("error.timesheet.autoLockRange", null,
                    "Auto-lock must be at least 1 day, or off");
        }
        if (roundingStepMinutes != 0 && roundingStepMinutes != 5 && roundingStepMinutes != 15 && roundingStepMinutes != 30) {
            throw new TimesheetDomainException("error.timesheet.roundingStep", null,
                    "Rounding step must be 0, 5, 15 or 30 minutes");
        }
        this.managersMaySelfApprove = managersMaySelfApprove;
        this.autoLockAfterDays = autoLockAfterDays;
        this.roundingStepMinutes = roundingStepMinutes;
        this.roundingMode = roundingMode == null ? RoundingMode.NEAREST : roundingMode;
    }

    public static TimesheetSettings withWorkflow(TimesheetSettings base, boolean selfApprove, Integer autoLock, int step,
                                                 RoundingMode mode) {
        base.managersMaySelfApprove = selfApprove;
        base.autoLockAfterDays = autoLock;
        base.roundingStepMinutes = step;
        base.roundingMode = mode == null ? RoundingMode.NEAREST : mode;
        return base;
    }

    /** TSH-10: whether approved weeks post their labor cost to the ledger, and which journal and accounts they use. */
    public void updateLedger(boolean enabled, java.util.UUID defaultCostAccountId, java.util.UUID laborAppliedAccountId,
                             String journalCode) {
        if (journalCode == null || journalCode.isBlank() || journalCode.trim().length() > 10) {
            throw new TimesheetDomainException("error.timesheet.journalCodeInvalid", null,
                    "The journal code is required and can have at most 10 characters");
        }
        this.ledgerPostingEnabled = enabled;
        this.defaultCostAccountId = defaultCostAccountId;
        this.laborAppliedAccountId = laborAppliedAccountId;
        this.journalCode = journalCode.trim().toUpperCase();
    }

    /** TSH-05 #8: the weekday the reminder job runs, in the company time zone. */
    public void updateReminder(boolean enabled, DayOfWeek weekday) {
        this.reminderEnabled = enabled;
        this.reminderWeekday = weekday == null ? this.reminderWeekday : weekday;
    }

    public boolean isReminderEnabled() { return reminderEnabled; }
    public DayOfWeek getReminderWeekday() { return reminderWeekday; }
    public boolean isLedgerPostingEnabled() { return ledgerPostingEnabled; }
    public java.util.UUID getDefaultCostAccountId() { return defaultCostAccountId; }
    public java.util.UUID getLaborAppliedAccountId() { return laborAppliedAccountId; }
    public String getJournalCode() { return journalCode; }
    public boolean isManagersMaySelfApprove() { return managersMaySelfApprove; }
    public Integer getAutoLockAfterDays() { return autoLockAfterDays; }
    public int getRoundingStepMinutes() { return roundingStepMinutes; }
    public RoundingMode getRoundingMode() { return roundingMode; }
    public CompanyId getCompanyId() { return companyId; }
    public TimeFormat getTimeFormat() { return timeFormat; }
    public DayOfWeek getWeekStartDay() { return weekStartDay; }
    public ZoneId getZone() { return zone; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public int getAllowFutureDays() { return allowFutureDays; }
    public int getMinutesPerDay() { return minutesPerDay; }
}
