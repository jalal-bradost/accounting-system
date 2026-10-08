package com.bradox.erp.timesheet.domain.core.rule;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeekCalendarTest {

    // 2026-10-08 is a Thursday
    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 8);

    @Test
    void saturdayStart() {
        assertEquals(LocalDate.of(2026, 10, 3), WeekCalendar.weekStart(THURSDAY, DayOfWeek.SATURDAY));
    }

    @Test
    void sundayStart() {
        assertEquals(LocalDate.of(2026, 10, 4), WeekCalendar.weekStart(THURSDAY, DayOfWeek.SUNDAY));
    }

    @Test
    void startDayItselfStaysPut() {
        assertEquals(LocalDate.of(2026, 10, 3), WeekCalendar.weekStart(LocalDate.of(2026, 10, 3), DayOfWeek.SATURDAY));
    }

    @Test
    void sevenDays() {
        var days = WeekCalendar.weekDates(LocalDate.of(2026, 10, 3));
        assertEquals(7, days.size());
        assertEquals(LocalDate.of(2026, 10, 9), days.get(6));
    }
}
