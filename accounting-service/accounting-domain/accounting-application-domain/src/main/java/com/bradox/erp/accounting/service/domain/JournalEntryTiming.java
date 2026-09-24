package com.bradox.erp.accounting.service.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Journal entries store a full {@link LocalDateTime}. Document-driven flows often only know a
 * business {@link LocalDate}; using {@link LocalDate#atStartOfDay()} stamps midnight and breaks
 * chronological ordering among same-day entries. These helpers keep the calendar day and stamp
 * the current clock time.
 */
public final class JournalEntryTiming {

    private JournalEntryTiming() {}

    /** Business calendar date + current local time. */
    public static LocalDateTime ofBusinessDate(LocalDate businessDate) {
        return ofBusinessDate(businessDate, Clock.systemDefaultZone());
    }

    public static LocalDateTime ofBusinessDate(LocalDate businessDate, Clock clock) {
        LocalDate day = businessDate != null ? businessDate : LocalDate.now(clock);
        return LocalDateTime.of(day, LocalTime.now(clock));
    }

    /**
     * If {@code dateTime} is null → now.
     * If it is exactly midnight → same calendar date with current clock time.
     * Otherwise leave the caller-supplied time untouched (e.g. cash/bank payments).
     */
    public static LocalDateTime ensureTimed(LocalDateTime dateTime) {
        return ensureTimed(dateTime, Clock.systemDefaultZone());
    }

    public static LocalDateTime ensureTimed(LocalDateTime dateTime, Clock clock) {
        if (dateTime == null) {
            return LocalDateTime.now(clock);
        }
        if (dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return dateTime.toLocalDate().atTime(LocalTime.now(clock));
        }
        return dateTime;
    }
}
