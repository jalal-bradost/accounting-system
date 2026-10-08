package com.bradox.erp.timesheet.domain.core.rule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** The timesheet week starts on a company setting (D8), not on the locale's first day. */
public final class WeekCalendar {

    private WeekCalendar() {
    }

    public static LocalDate weekStart(LocalDate date, DayOfWeek startDay) {
        return date.with(TemporalAdjusters.previousOrSame(startDay));
    }

    public static List<LocalDate> weekDates(LocalDate weekStart) {
        List<LocalDate> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            days.add(weekStart.plusDays(i));
        }
        return days;
    }
}
