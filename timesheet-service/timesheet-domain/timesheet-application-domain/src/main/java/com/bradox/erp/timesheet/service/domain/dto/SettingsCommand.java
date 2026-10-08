package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.TimeFormat;

import java.time.DayOfWeek;
import java.util.UUID;

/** Ledger fields need {@code tsh.posting.manage}; the rest need {@code tsh.settings.manage}. */
public record SettingsCommand(TimeFormat timeFormat, DayOfWeek weekStartDay, String timezone,
                              Boolean approvalRequired, Integer allowFutureDays, Integer minutesPerDay,
                              Boolean managersMaySelfApprove, Integer autoLockAfterDays, Boolean clearAutoLock,
                              Integer roundingStepMinutes, RoundingMode roundingMode, Boolean ledgerPostingEnabled,
                              UUID defaultCostAccountId, UUID laborAppliedAccountId, String journalCode,
                              Boolean reminderEnabled, DayOfWeek reminderWeekday) {

    /** Everything up to the ledger options; the reminder keeps its current values. */
    public SettingsCommand(TimeFormat timeFormat, DayOfWeek weekStartDay, String timezone, Boolean approvalRequired,
                           Integer allowFutureDays, Integer minutesPerDay, Boolean managersMaySelfApprove,
                           Integer autoLockAfterDays, Boolean clearAutoLock, Integer roundingStepMinutes,
                           RoundingMode roundingMode, Boolean ledgerPostingEnabled, UUID defaultCostAccountId,
                           UUID laborAppliedAccountId, String journalCode) {
        this(timeFormat, weekStartDay, timezone, approvalRequired, allowFutureDays, minutesPerDay, managersMaySelfApprove,
                autoLockAfterDays, clearAutoLock, roundingStepMinutes, roundingMode, ledgerPostingEnabled,
                defaultCostAccountId, laborAppliedAccountId, journalCode, null, null);
    }

    /** The base settings only; every option beyond them keeps its current value. */
    public SettingsCommand(TimeFormat timeFormat, DayOfWeek weekStartDay, String timezone, Boolean approvalRequired,
                           Integer allowFutureDays, Integer minutesPerDay) {
        this(timeFormat, weekStartDay, timezone, approvalRequired, allowFutureDays, minutesPerDay, null, null, null,
                null, null, null, null, null, null, null, null);
    }
}
