package com.bradox.delin.platform.activity;

/** Open vs completed filter for the activity inbox list. */
public enum ActivityInboxStatus {
    OPEN,
    DONE,
    ALL;

    public static ActivityInboxStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        return ActivityInboxStatus.valueOf(raw.trim().toUpperCase());
    }
}
