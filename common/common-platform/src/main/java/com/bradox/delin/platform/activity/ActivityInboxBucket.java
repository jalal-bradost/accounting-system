package com.bradox.delin.platform.activity;

import java.time.LocalDate;

/** Due-date bucket for open ACTIVITY_TODO rows relative to an as-of date. */
public enum ActivityInboxBucket {
    LATE,
    TODAY,
    FUTURE;

    public static ActivityInboxBucket of(LocalDate dueDate, LocalDate asOf) {
        if (dueDate == null || dueDate.isAfter(asOf)) {
            return FUTURE;
        }
        if (dueDate.isEqual(asOf)) {
            return TODAY;
        }
        return LATE;
    }

    public static ActivityInboxBucket parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ActivityInboxBucket.valueOf(raw.trim().toUpperCase());
    }
}
