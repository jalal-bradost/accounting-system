package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.TimeFormat;

import java.time.DayOfWeek;
import java.util.UUID;

public record SettingsResponse(TimeFormat timeFormat, DayOfWeek weekStartDay, String timezone,
                               boolean approvalRequired, int allowFutureDays, int minutesPerDay,
                               boolean managersMaySelfApprove, Integer autoLockAfterDays, int roundingStepMinutes,
                               RoundingMode roundingMode, boolean ledgerPostingEnabled, UUID defaultCostAccountId,
                               UUID laborAppliedAccountId, String journalCode, boolean reminderEnabled,
                               DayOfWeek reminderWeekday) {
}
