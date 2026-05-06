package com.jalaldeveloper.accountingsystem.platform.activity;

/** Type of an entry in the chatter feed of any record. */
public enum ActivityKind {
    /** Internal log note (Markdown body). */
    NOTE,
    /** External-facing message (e.g. emailed to the partner). */
    COMMENT,
    /** Scheduled to-do with assignee + due date. */
    ACTIVITY_TODO,
    /** Auto-generated message from the system (state changes, integrations). */
    SYSTEM,
    /** Pointer to a related entry in the audit log. */
    AUDIT_LINK
}
